package com.contractiq.test;

import com.contractiq.ai.LlmExtractionResult;
import com.contractiq.ai.OpenAiCompatibleLlmService;
import com.contractiq.ai.RuleBasedFallbackExtractor;
import com.contractiq.dto.ContractExtractionDto;
import com.contractiq.pdf.ExtractedPdfContent;
import com.contractiq.util.EnvLoader;
import com.contractiq.util.JsonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LlmExtractionParsingTest {

    private Map<String, String> originalEnvironment;

    @AfterEach
    void restoreEnvironment() throws Exception {
        if (originalEnvironment != null) {
            environment().clear();
            environment().putAll(originalEnvironment);
        }
    }

    @Test
    void testParseStructuredLlmJson() {
        String json = """
            {
              "contractTitle": "Master Software Agreement",
              "contractType": "SaaS Agreement",
              "partyFirst": "Alpha Systems",
              "partySecond": "Beta Global",
              "effectiveDate": "2025-01-01",
              "expirationDate": "2026-01-01",
              "autoRenewal": true,
              "renewalNoticePeriodDays": 30,
              "renewalTerms": "Renews for 1 year",
              "paymentTerms": "Net 30",
              "confidenceScore": 0.95,
              "obligations": [
                {
                  "obligationTitle": "Quarterly Invoicing",
                  "description": "Pay within 30 days of invoice",
                  "responsibleParty": "Beta Global",
                  "deadlineDate": "2025-04-01",
                  "frequency": "QUARTERLY",
                  "category": "PAYMENT",
                  "sourceExcerpt": "Section 4",
                  "pageNumber": 1,
                  "confidence": 0.92
                }
              ]
            }
            """;

        ContractExtractionDto dto = JsonUtil.fromJson(json, ContractExtractionDto.class);
        assertNotNull(dto);
        assertEquals("Master Software Agreement", dto.getContractTitle());
        assertEquals("Alpha Systems", dto.getPartyFirst());
        assertEquals("Beta Global", dto.getPartySecond());
        assertEquals("2025-01-01", dto.getEffectiveDate());
        assertTrue(dto.getAutoRenewal());
        assertEquals(1, dto.getObligations().size());
        assertEquals("PAYMENT", dto.getObligations().get(0).getCategory());
    }

    @Test
    void testRuleBasedFallbackExtractor() {
        String syntheticText = """
            ENTERPRISE SERVICES AGREEMENT
            This Agreement is entered into by and between CloudScale Inc. and Horizon Global LLC.
            Effective as of 2025-03-01. Expiration date: 2026-03-01.
            This agreement shall automatically renew for successive periods.
            Either party may terminate by providing at least 60 days written notice.
            Section 4. Customer shall pay quarterly subscription invoices Net 30 days.
            Section 9. Provider shall deliver annual SOC 2 audit compliance reports.
            """;

        ExtractedPdfContent content = new ExtractedPdfContent(syntheticText, 1, Map.of(1, syntheticText));
        RuleBasedFallbackExtractor extractor = new RuleBasedFallbackExtractor();

        LlmExtractionResult result = extractor.extract("agreement.pdf", content);

        assertNotNull(result);
        assertFalse(result.isRealLlm(), "Must honestly declare realLlm=false for heuristic extractor");
        assertTrue(result.getModelUsed().contains("Heuristic"));

        ContractExtractionDto data = result.getContractData();
        assertEquals("CloudScale Inc.", data.getPartyFirst());
        assertEquals("Horizon Global LLC", data.getPartySecond());
        assertEquals("2025-03-01", data.getEffectiveDate());
        assertEquals("2026-03-01", data.getExpirationDate());
        assertTrue(data.getAutoRenewal());
        assertEquals(60, data.getRenewalNoticePeriodDays());
        assertFalse(data.getObligations().isEmpty());
    }

    @Test
    void testRuleBasedFallbackDoesNotInventObligationsWhenNoEvidenceExists() {
        String syntheticText = "This document records a meeting between the parties and contains no contractual duties.";
        ExtractedPdfContent content = new ExtractedPdfContent(syntheticText, 1, Map.of(1, syntheticText));

        ContractExtractionDto data = new RuleBasedFallbackExtractor().extract("notes.pdf", content).getContractData();

        assertNull(data.getPartyFirst());
        assertNull(data.getPartySecond());
        assertTrue(data.getObligations().isEmpty(), "Heuristic mode must not fabricate a generic obligation.");
    }

    @Test
    void testProviderHttpErrorUsesClearlyLabelledHeuristicFallback() throws Exception {
        Map<String, String> environment = environment();
        originalEnvironment = new HashMap<>(environment);
        environment.put("LLM_API_KEY", "test-key");
        environment.put("LLM_API_URL", "https://llm.example.test/v1/chat/completions");
        environment.put("LLM_MODEL", "test-model");
        environment.put("DEMO_MODE", "false");

        HttpClient client = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn("provider failure");
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        String contractText = "Customer shall pay quarterly invoices within 30 days.";
        ExtractedPdfContent content = new ExtractedPdfContent(contractText, 1, Map.of(1, contractText));
        OpenAiCompatibleLlmService service = new OpenAiCompatibleLlmService(client, new RuleBasedFallbackExtractor());

        LlmExtractionResult result = service.extractContractObligations("sample.pdf", content);

        assertFalse(result.isRealLlm());
        assertTrue(result.getModelUsed().contains("Fallback after HTTP 500"));
        assertTrue(result.getContractData().getObligations().stream()
                .allMatch(obligation -> obligation.getSourceExcerpt().contains("shall pay")));
        verify(client).send(any(), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> environment() throws Exception {
        Field field = EnvLoader.class.getDeclaredField("envMap");
        field.setAccessible(true);
        return (Map<String, String>) field.get(null);
    }
}
