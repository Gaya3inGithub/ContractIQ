package com.contractiq.ai;

import com.contractiq.dto.ContractExtractionDto;
import com.contractiq.dto.ObligationExtractionDto;
import com.contractiq.pdf.ExtractedPdfContent;
import com.contractiq.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent heuristic rule-based contract extractor.
 * Used when real LLM is unconfigured, demo mode is enabled, or as offline fallback.
 */
public class RuleBasedFallbackExtractor {
    private static final Logger logger = LoggerFactory.getLogger(RuleBasedFallbackExtractor.class);

    public LlmExtractionResult extract(String fileName, ExtractedPdfContent pdfContent) {
        long start = System.currentTimeMillis();
        String text = pdfContent.getFullText();

        ContractExtractionDto dto = new ContractExtractionDto();
        dto.setContractTitle(extractTitle(fileName, text));
        dto.setContractType(classifyContractType(text, fileName));

        String[] parties = extractParties(text);
        dto.setPartyFirst(parties[0]);
        dto.setPartySecond(parties[1]);

        dto.setEffectiveDate(extractEffectiveDate(text));
        dto.setExpirationDate(extractExpirationDate(text));

        boolean autoRenew = containsPattern(text, "(?i)automatically\\s+renew|auto[- ]renew|successive\\s+periods");
        dto.setAutoRenewal(autoRenew);

        Integer noticeDays = extractNoticePeriodDays(text);
        dto.setRenewalNoticePeriodDays(noticeDays);

        dto.setRenewalTerms(extractExcerpt(text, "(?i)(renewal|renew|term of agreement)[^.]{10,200}\\."));
        dto.setPaymentTerms(extractExcerpt(text, "(?i)(payment|fees|invoicing|compensation|net\\s+\\d+)[^.]{10,250}\\."));
        dto.setPenaltiesClause(extractExcerpt(text, "(?i)(late fee|penalty|interest rate|liquidated damages)[^.]{10,250}\\."));
        dto.setTerminationConditions(extractExcerpt(text, "(?i)(termination|terminate|breach of contract)[^.]{10,250}\\."));

        dto.setConfidenceScore(new BigDecimal("0.85"));
        dto.setNotes("Extracted via RuleBasedFallbackExtractor (Offline / Heuristic Mode). All values are AI suggestions and require human verification.");

        // Extract Obligations
        List<ObligationExtractionDto> obligations = extractObligations(pdfContent, parties[0], parties[1]);
        dto.setObligations(obligations);

        long duration = System.currentTimeMillis() - start;
        return new LlmExtractionResult(dto, "Heuristic-Rule-Engine-v1", false, duration,
                "Demo/Fallback mode active. Configured LLM_API_KEY was not provided or fallback was triggered.");
    }

    private String extractTitle(String fileName, String text) {
        // Look in first 500 characters
        String header = text.length() > 600 ? text.substring(0, 600) : text;
        Pattern p = Pattern.compile("(?i)(?:THIS\\s+)?([A-Z][A-Za-z0-9\\s]{4,60}\\s+(?:AGREEMENT|CONTRACT|TERMS OF SERVICE|SERVICES AGREEMENT|NON-DISCLOSURE AGREEMENT))");
        Matcher m = p.matcher(header);
        if (m.find()) {
            return m.group(1).trim();
        }
        // Fallback to cleaned file name
        String clean = fileName.replace(".pdf", "").replace("_", " ").replace("-", " ");
        return clean.substring(0, 1).toUpperCase() + clean.substring(1);
    }

    private String classifyContractType(String text, String fileName) {
        String lower = (text + " " + fileName).toLowerCase();
        if (lower.contains("software as a service") || lower.contains("saas agreement") || lower.contains("subscription")) return "SaaS Agreement";
        if (lower.contains("non-disclosure") || lower.contains("confidentiality")) return "Non-Disclosure Agreement (NDA)";
        if (lower.contains("master services") || lower.contains("msa")) return "Master Services Agreement";
        if (lower.contains("employment") || lower.contains("employee")) return "Employment Agreement";
        if (lower.contains("vendor") || lower.contains("supplier") || lower.contains("procurement")) return "Vendor Agreement";
        if (lower.contains("license") || lower.contains("licensing")) return "Software License Agreement";
        if (lower.contains("lease") || lower.contains("tenancy")) return "Commercial Lease Agreement";
        return "Commercial Contract";
    }

    private String[] extractParties(String text) {
        String p1 = "Party A";
        String p2 = "Party B";

        Pattern p = Pattern.compile("(?i)(?:by and between|between)\\s+([A-Z][A-Za-z0-9&.\\s]{2,50}?)\\s+(?:and|with)\\s+([A-Z][A-Za-z0-9&.\\s]{2,50}?)(?:[.\\n\\r]|\\s*\\(|\\s*,\\s*an?|$)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            p1 = cleanParty(m.group(1));
            p2 = cleanParty(m.group(2));
        }
        return new String[]{p1, p2};
    }

    private String cleanParty(String party) {
        if (party == null) return "";
        String clean = party.trim();
        if (clean.endsWith(",")) clean = clean.substring(0, clean.length() - 1).trim();
        return clean;
    }

    private String extractEffectiveDate(String text) {
        Pattern p = Pattern.compile("(?i)(?:effective\\s+(?:as\\s+of|date:?)|dated\\s+as\\s+of)\\s+([A-Za-z0-9,\\s]{6,25})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            LocalDate d = DateUtil.parseSafe(m.group(1).trim());
            if (d != null) return DateUtil.formatIso(d);
        }
        // Fallback to ISO date pattern search
        Pattern pIso = Pattern.compile("\\b(202[4-9]-\\d{2}-\\d{2})\\b");
        Matcher mIso = pIso.matcher(text);
        if (mIso.find()) {
            return mIso.group(1);
        }
        return null; // Strict rule: return null for ambiguous dates
    }

    private String extractExpirationDate(String text) {
        Pattern p = Pattern.compile("(?i)(?:expire[s]?\\s+on|expiration\\s+date:?|terminat(?:es|ion)\\s+on)\\s+([A-Za-z0-9,\\s\\-]{6,25})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            LocalDate d = DateUtil.parseSafe(m.group(1).trim());
            if (d != null) return DateUtil.formatIso(d);
        }
        return null;
    }

    private Integer extractNoticePeriodDays(String text) {
        Pattern p = Pattern.compile("(?i)(?:at\\s+least|prior\\s+notice\\s+of)\\s+(\\d{1,3})\\s+(?:calendar\\s+)?days");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private List<ObligationExtractionDto> extractObligations(ExtractedPdfContent pdfContent, String p1, String p2) {
        List<ObligationExtractionDto> list = new ArrayList<>();
        String text = pdfContent.getFullText();

        // Check for payment obligation
        Pattern pPay = Pattern.compile("(?i)(?:shall\\s+pay|payment\\s+of|invoicing)[^.]{10,250}\\.");
        Matcher mPay = pPay.matcher(text);
        if (mPay.find()) {
            ObligationExtractionDto o = new ObligationExtractionDto();
            o.setObligationTitle("Contractual Payment & Fee Schedule");
            o.setDescription(mPay.group(0).trim());
            o.setResponsibleParty(p2 != null ? p2 : "Customer / Client");
            o.setCategory("PAYMENT");
            o.setFrequency("MONTHLY");
            o.setConfidence(new BigDecimal("0.90"));
            o.setSourceExcerpt(mPay.group(0).trim());
            o.setPageNumber(findPageForText(pdfContent, mPay.group(0)));
            list.add(o);
        }

        // Check for renewal / termination notice obligation
        Pattern pRen = Pattern.compile("(?i)(?:written\\s+notice|intent\\s+to\\s+terminate|notice\\s+of\\s+non-renewal)[^.]{10,250}\\.");
        Matcher mRen = pRen.matcher(text);
        if (mRen.find()) {
            ObligationExtractionDto o = new ObligationExtractionDto();
            o.setObligationTitle("Notice of Termination or Non-Renewal");
            o.setDescription(mRen.group(0).trim());
            o.setResponsibleParty(p2 != null ? p2 : "Either Party");
            o.setCategory("TERMINATION");
            o.setFrequency("ONE_TIME");
            o.setConfidence(new BigDecimal("0.88"));
            o.setSourceExcerpt(mRen.group(0).trim());
            o.setPageNumber(findPageForText(pdfContent, mRen.group(0)));
            list.add(o);
        }

        // Check for reporting / audit / compliance obligation
        Pattern pComp = Pattern.compile("(?i)(?:compliance|audit|soc\\s*2|confidentiality|report\\s+annually)[^.]{10,250}\\.");
        Matcher mComp = pComp.matcher(text);
        if (mComp.find()) {
            ObligationExtractionDto o = new ObligationExtractionDto();
            o.setObligationTitle("Regulatory & Security Compliance Reporting");
            o.setDescription(mComp.group(0).trim());
            o.setResponsibleParty(p1 != null ? p1 : "Service Provider");
            o.setCategory("COMPLIANCE");
            o.setFrequency("ANNUALLY");
            o.setConfidence(new BigDecimal("0.85"));
            o.setSourceExcerpt(mComp.group(0).trim());
            o.setPageNumber(findPageForText(pdfContent, mComp.group(0)));
            list.add(o);
        }

        // If no explicit clauses found, add general obligation
        if (list.isEmpty()) {
            ObligationExtractionDto o = new ObligationExtractionDto();
            o.setObligationTitle("Standard Contractual Performance Obligation");
            o.setDescription("Perform duties, services, or delivery in accordance with the contract specifications.");
            o.setResponsibleParty(p1 != null ? p1 : "Contractor");
            o.setCategory("DELIVERY");
            o.setFrequency("ONE_TIME");
            o.setConfidence(new BigDecimal("0.75"));
            o.setSourceExcerpt("Extracted from introductory provisions.");
            o.setPageNumber(1);
            list.add(o);
        }

        return list;
    }

    private int findPageForText(ExtractedPdfContent content, String snippet) {
        if (snippet == null || snippet.isEmpty()) return 1;
        String search = snippet.length() > 30 ? snippet.substring(0, 30) : snippet;
        for (Map.Entry<Integer, String> entry : content.getPageTexts().entrySet()) {
            if (entry.getValue().contains(search)) {
                return entry.getKey();
            }
        }
        return 1;
    }

    private boolean containsPattern(String text, String regex) {
        return Pattern.compile(regex).matcher(text).find();
    }

    private String extractExcerpt(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (m.find()) {
            return m.group(0).trim();
        }
        return null;
    }
}
