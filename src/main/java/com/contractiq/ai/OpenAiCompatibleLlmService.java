package com.contractiq.ai;

import com.contractiq.config.AppConfig;
import com.contractiq.dto.ContractExtractionDto;
import com.contractiq.pdf.ExtractedPdfContent;
import com.contractiq.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleLlmService implements LlmService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAiCompatibleLlmService.class);

    private final HttpClient httpClient;
    private final RuleBasedFallbackExtractor fallbackExtractor;

    public OpenAiCompatibleLlmService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.fallbackExtractor = new RuleBasedFallbackExtractor();
    }

    public OpenAiCompatibleLlmService(HttpClient httpClient, RuleBasedFallbackExtractor fallbackExtractor) {
        this.httpClient = httpClient;
        this.fallbackExtractor = fallbackExtractor;
    }

    @Override
    public boolean isConfigured() {
        return AppConfig.isLlmConfigured() && !AppConfig.isDemoMode();
    }

    @Override
    public String getProviderName() {
        if (!isConfigured()) {
            return "RuleBasedFallbackEngine (Offline / Demo)";
        }
        return "OpenAI-Compatible (" + AppConfig.getLlmModel() + ")";
    }

    @Override
    public LlmExtractionResult extractContractObligations(String fileName, ExtractedPdfContent pdfContent) {
        if (!isConfigured()) {
            logger.info("Real LLM is not configured or DEMO_MODE=true. Utilizing RuleBasedFallbackExtractor.");
            return fallbackExtractor.extract(fileName, pdfContent);
        }

        long startTime = System.currentTimeMillis();
        String apiKey = AppConfig.getLlmApiKey();
        String apiUrl = AppConfig.getLlmApiUrl();
        String model = AppConfig.getLlmModel();
        int timeoutSeconds = AppConfig.getLlmTimeoutSeconds();

        // Do not log credentials, request payloads, or provider URLs. Provider
        // URLs can contain query-string credentials in some deployments.
        logger.info("Initiating LLM contract analysis [Model: {}].", model);

        String prompt = buildPrompt(fileName, pdfContent);

        try {
            String jsonPayload = buildRequestBody(model, prompt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = sendWithRetry(request, 1);

            int statusCode = response.statusCode();
            if (statusCode == 200) {
                ContractExtractionDto dto = parseLlmResponse(response.body());
                long duration = System.currentTimeMillis() - startTime;
                logger.info("LLM extraction succeeded in {} ms with {} obligations found.", duration, dto.getObligations().size());
                return new LlmExtractionResult(dto, model, true, duration, "Real LLM extraction completed successfully.");
            } else if (statusCode == 429) {
                logger.warn("LLM Provider Rate Limit (HTTP 429) encountered. Falling back to heuristic extractor.");
                LlmExtractionResult res = fallbackExtractor.extract(fileName, pdfContent);
                return new LlmExtractionResult(res.getContractData(), "Fallback after HTTP 429 Rate Limit", false,
                        System.currentTimeMillis() - startTime, "API Rate Limit reached. Heuristic extractor served as fallback.");
            } else {
                logger.error("LLM Provider returned HTTP error status {}. Falling back to the heuristic extractor.", statusCode);
                LlmExtractionResult res = fallbackExtractor.extract(fileName, pdfContent);
                return new LlmExtractionResult(res.getContractData(), "Fallback after HTTP " + statusCode, false,
                        System.currentTimeMillis() - startTime, "API returned HTTP " + statusCode + ". Fallback engine used.");
            }

        } catch (Exception e) {
            logger.error("LLM contract extraction failed ({}). Falling back to rule engine.",
                    e.getClass().getSimpleName());
            LlmExtractionResult res = fallbackExtractor.extract(fileName, pdfContent);
            return new LlmExtractionResult(res.getContractData(), "Fallback after error: " + e.getClass().getSimpleName(),
                    false, System.currentTimeMillis() - startTime,
                    "The LLM provider request failed. The heuristic extractor was used instead.");
        }
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request, int retriesLeft) throws IOException, InterruptedException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            if (retriesLeft > 0) {
                logger.warn("Transient network error during LLM request. Retrying once.");
                Thread.sleep(1000);
                return sendWithRetry(request, retriesLeft - 1);
            }
            throw e;
        }
    }

    private String buildPrompt(String fileName, ExtractedPdfContent pdfContent) {
        String excerpt = pdfContent.getExcerpt(18000); // Send up to 18,000 characters safely
        return """
            You are ContractIQ's Legal AI Assistant. Analyze the contract text below and extract structured obligations, renewal terms, and contract metadata into STRICT JSON format.

            RULES:
            1. Represent known dates as ISO 'YYYY-MM-DD'.
            2. If a date is ambiguous, missing, or calculated, use null. NEVER invent dates.
            3. Do not invent obligations, penalties, or parties.
            4. Flag uncertain items with confidence < 0.85.
            5. Return ONLY a single raw JSON object matching the schema below (no markdown ticks, no preamble).

            JSON SCHEMA EXPECTED:
            {
              "contractTitle": "String",
              "contractType": "SaaS Agreement | Non-Disclosure Agreement | Master Services Agreement | Vendor Agreement | Other",
              "partyFirst": "String",
              "partySecond": "String",
              "effectiveDate": "YYYY-MM-DD or null",
              "expirationDate": "YYYY-MM-DD or null",
              "autoRenewal": boolean,
              "renewalNoticePeriodDays": integer or null,
              "renewalTerms": "String or null",
              "paymentTerms": "String or null",
              "penaltiesClause": "String or null",
              "terminationConditions": "String or null",
              "confidenceScore": 0.95,
              "notes": "String",
              "obligations": [
                {
                  "obligationTitle": "String",
                  "description": "String",
                  "responsibleParty": "String",
                  "deadlineDate": "YYYY-MM-DD or null",
                  "frequency": "ONE_TIME | MONTHLY | QUARTERLY | ANNUALLY",
                  "category": "PAYMENT | COMPLIANCE | DELIVERY | REPORTING | TERMINATION | CONFIDENTIALITY | OTHER",
                  "sourceExcerpt": "Exact text from contract",
                  "pageNumber": 1,
                  "confidence": 0.90
                }
              ]
            }

            CONTRACT FILE: %s
            CONTRACT TEXT:
            %s
            """.formatted(fileName, excerpt);
    }

    private String buildRequestBody(String model, String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.1);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "You are an expert contract lawyer and AI parser. You always return strict valid JSON matching the requested schema with no commentary."),
                Map.of("role", "user", "content", prompt)
        ));
        return JsonUtil.toJson(body);
    }

    private ContractExtractionDto parseLlmResponse(String rawResponseBody) {
        try {
            JsonNode root = JsonUtil.readTree(rawResponseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                // Strip markdown formatting if model wrapped in ```json ... ```
                content = cleanMarkdownJson(content);
                return JsonUtil.fromJson(content, ContractExtractionDto.class);
            }
            throw new RuntimeException("No choices returned in LLM response.");
        } catch (Exception e) {
            logger.error("Failed to parse the LLM JSON response.");
            throw new RuntimeException("Malformed response from LLM.", e);
        }
    }

    private String cleanMarkdownJson(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
