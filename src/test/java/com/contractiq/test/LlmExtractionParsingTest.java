package com.contractiq.test;

import com.contractiq.ai.LlmExtractionResult;
import com.contractiq.ai.RuleBasedFallbackExtractor;
import com.contractiq.dto.ContractExtractionDto;
import com.contractiq.pdf.ExtractedPdfContent;
import com.contractiq.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmExtractionParsingTest {

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
}
