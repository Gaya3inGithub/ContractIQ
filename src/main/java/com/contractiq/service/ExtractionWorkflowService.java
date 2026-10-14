package com.contractiq.service;

import com.contractiq.ai.LlmExtractionResult;
import com.contractiq.ai.LlmService;
import com.contractiq.ai.OpenAiCompatibleLlmService;
import com.contractiq.config.AppConfig;
import com.contractiq.dao.ActivityDao;
import com.contractiq.dao.ContractDao;
import com.contractiq.dao.ObligationDao;
import com.contractiq.dto.ContractExtractionDto;
import com.contractiq.dto.ObligationExtractionDto;
import com.contractiq.exception.StorageException;
import com.contractiq.exception.ValidationException;
import com.contractiq.model.ActivityLog;
import com.contractiq.model.Contract;
import com.contractiq.model.Obligation;
import com.contractiq.pdf.ExtractedPdfContent;
import com.contractiq.pdf.PdfExtractionService;
import com.contractiq.pdf.PdfValidationService;
import com.contractiq.storage.ContractStorageService;
import com.contractiq.storage.StoredFile;
import com.contractiq.util.DateUtil;
import com.contractiq.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExtractionWorkflowService {
    private static final Logger logger = LoggerFactory.getLogger(ExtractionWorkflowService.class);

    private final PdfValidationService pdfValidationService;
    private final PdfExtractionService pdfExtractionService;
    private final LlmService llmService;
    private final ContractDao contractDao;
    private final ObligationDao obligationDao;
    private final ActivityDao activityDao;
    private final ContractStorageService storageService;

    public ExtractionWorkflowService() {
        this.pdfValidationService = new PdfValidationService();
        this.pdfExtractionService = new PdfExtractionService();
        this.llmService = new OpenAiCompatibleLlmService();
        this.contractDao = new ContractDao();
        this.obligationDao = new ObligationDao();
        this.activityDao = new ActivityDao();
        this.storageService = new ContractStorageService();
    }

    public ExtractionWorkflowService(PdfValidationService pdfValidationService,
                                   PdfExtractionService pdfExtractionService,
                                   LlmService llmService,
                                   ContractDao contractDao,
                                   ObligationDao obligationDao,
                                   ActivityDao activityDao) {
        this.pdfValidationService = pdfValidationService;
        this.pdfExtractionService = pdfExtractionService;
        this.llmService = llmService;
        this.contractDao = contractDao;
        this.obligationDao = obligationDao;
        this.activityDao = activityDao;
        this.storageService = new ContractStorageService();
    }

    public ExtractionWorkflowService(PdfValidationService pdfValidationService,
                                   PdfExtractionService pdfExtractionService,
                                   LlmService llmService,
                                   ContractDao contractDao,
                                   ObligationDao obligationDao,
                                   ActivityDao activityDao,
                                   ContractStorageService storageService) {
        this.pdfValidationService = pdfValidationService;
        this.pdfExtractionService = pdfExtractionService;
        this.llmService = llmService;
        this.contractDao = contractDao;
        this.obligationDao = obligationDao;
        this.activityDao = activityDao;
        this.storageService = storageService;
    }

    /**
     * Executes the end-to-end contract ingestion, text extraction, AI analysis,
     * and database persistence with pending human approval.
     */
    public Contract processContractUpload(String originalFileName, InputStream fileStream, String uploaderIp) throws SQLException {
        long startTime = System.currentTimeMillis();
        String safeOriginalName = SecurityUtil.sanitizeFileName(originalFileName);
        String storageKey = null;
        File targetFile = null;
        boolean completed = false;

        try {
            StoredFile storedFile;
            try {
                storedFile = storageService.saveUploadedFile(safeOriginalName, fileStream);
            } catch (Exception e) {
                throw new StorageException("Failed to save uploaded contract file.", e);
            }
            storageKey = storedFile.getStorageKey();
            try {
                targetFile = storageService.materializeToTempFile(storageKey, safeOriginalName);
            } catch (Exception e) {
                throw new StorageException("Failed to prepare the uploaded PDF for validation and extraction.", e);
            }

            logger.info("Starting ingestion workflow for '{}' -> '{}'", originalFileName, storageKey);

            // Step 1: Validate PDF format, size, and magic bytes
            pdfValidationService.validatePdfFile(targetFile, originalFileName);
            byte[] fileBytes;
            try {
                fileBytes = Files.readAllBytes(targetFile.toPath());
            } catch (Exception e) {
                throw new StorageException("Failed to read the validated contract file.", e);
            }
            String sha256 = SecurityUtil.computeSha256(fileBytes);

            // Step 2: Extract text via Apache PDFBox
            ExtractedPdfContent pdfContent = pdfExtractionService.extractText(targetFile);

            // Step 3: AI Analysis via LLM Service (or heuristic fallback)
            LlmExtractionResult aiResult = llmService.extractContractObligations(safeOriginalName, pdfContent);
            ContractExtractionDto extractedDto = aiResult.getContractData();

        // Step 4: Convert extraction DTO to Contract entity
        Contract contract = new Contract();
        contract.setFileName(safeOriginalName);
        contract.setStoredFilePath(storageKey);
        contract.setFileSizeBytes((long) fileBytes.length);
        contract.setFileHashSha256(sha256);

        contract.setContractTitle(extractedDto.getContractTitle() != null ? extractedDto.getContractTitle() : safeOriginalName);
        contract.setContractType(extractedDto.getContractType() != null ? extractedDto.getContractType() : "General Agreement");
        contract.setPartyFirst(extractedDto.getPartyFirst());
        contract.setPartySecond(extractedDto.getPartySecond());

        LocalDate effDate = DateUtil.parseSafe(extractedDto.getEffectiveDate());
        LocalDate expDate = DateUtil.parseSafe(extractedDto.getExpirationDate());
        contract.setEffectiveDate(effDate);
        contract.setExpirationDate(expDate);

        contract.setAutoRenewal(extractedDto.getAutoRenewal() != null && extractedDto.getAutoRenewal());
        contract.setRenewalNoticePeriodDays(extractedDto.getRenewalNoticePeriodDays());
        contract.setRenewalNoticeDeadline(DateUtil.calculateNoticeDeadline(expDate, extractedDto.getRenewalNoticePeriodDays()));

        contract.setRenewalTerms(extractedDto.getRenewalTerms());
        contract.setPaymentTerms(extractedDto.getPaymentTerms());
        contract.setPenaltiesClause(extractedDto.getPenaltiesClause());
        contract.setTerminationConditions(extractedDto.getTerminationConditions());

        contract.setExtractionStatus("EXTRACTED");
        // CRITICAL REQUIREMENT: AI-generated records ALWAYS start in NEEDS_REVIEW.
        // AI extraction NEVER automatically approves a contract or obligation.
        contract.setReviewStatus("NEEDS_REVIEW");
        contract.setConfidenceScore(extractedDto.getConfidenceScore() != null ? extractedDto.getConfidenceScore() : new BigDecimal("0.90"));
        contract.setReviewNotes((aiResult.isRealLlm() ? "LLM" : "Heuristic") + " extraction completed by "
                + aiResult.getModelUsed() + ". Awaiting human review.");
        contract.setRawTextExcerpt(pdfContent.getExcerpt(4000));

        // Step 6: Persist Contract in MySQL
        Long contractId = contractDao.create(contract);
        contract.setId(contractId);

        // Step 7: Map and persist Obligations in MySQL with PENDING approval status
        List<Obligation> obligations = new ArrayList<>();
        if (extractedDto.getObligations() != null) {
            for (ObligationExtractionDto oDto : extractedDto.getObligations()) {
                Obligation ob = new Obligation();
                ob.setContractId(contractId);
                ob.setObligationTitle(oDto.getObligationTitle() != null ? oDto.getObligationTitle() : "Contractual Obligation");
                ob.setDescription(oDto.getDescription() != null ? oDto.getDescription() : "");
                ob.setResponsibleParty(oDto.getResponsibleParty() != null ? oDto.getResponsibleParty() : "Unspecified");
                ob.setDeadlineDate(DateUtil.parseSafe(oDto.getDeadlineDate()));
                ob.setFrequency(oDto.getFrequency());
                ob.setCategory(oDto.getCategory() != null ? oDto.getCategory() : "OTHER");
                ob.setSourceExcerpt(oDto.getSourceExcerpt());
                ob.setPageNumber(oDto.getPageNumber() != null ? oDto.getPageNumber() : 1);
                ob.setConfidence(oDto.getConfidence() != null ? oDto.getConfidence() : new BigDecimal("0.85"));

                // MANDATORY HUMAN APPROVAL WORKFLOW
                ob.setApprovalStatus("PENDING");
                obligations.add(ob);
            }
        }

        if (!obligations.isEmpty()) {
            obligations = obligationDao.createBatch(obligations, contractId);
        }
        contract.setObligations(obligations);

        // Step 8: Log audit activity records
        activityDao.log(new ActivityLog(contractId, "CONTRACT_UPLOADED",
                "Contract file '" + safeOriginalName + "' (" + (fileBytes.length / 1024) + " KB) uploaded.", "User", uploaderIp));

        activityDao.log(new ActivityLog(contractId, "TEXT_EXTRACTED",
                "PDF text extracted: " + pdfContent.getPageCount() + " pages, " + pdfContent.getCharacterCount() + " characters.", "PDFBox Stripper", null));

        String aiNote = String.format("AI analysis by %s (%d ms). Extracted %d suggested obligations. Status: PENDING HUMAN REVIEW.",
                aiResult.getModelUsed(), aiResult.getExecutionTimeMs(), obligations.size());
        activityDao.log(new ActivityLog(contractId, "AI_ANALYZED", aiNote, "ContractIQ AI", null));

        long totalDuration = System.currentTimeMillis() - startTime;
        logger.info("Ingestion workflow for contract #{} completed in {} ms.", contractId, totalDuration);

            completed = true;
            return contract;
        } finally {
            if (!completed) {
                if (storageKey != null) {
                    storageService.deleteIfExists(storageKey);
                }
                if (targetFile != null && targetFile.exists() && !targetFile.getAbsolutePath().equals(storageKey)) {
                    try {
                        Files.deleteIfExists(targetFile.toPath());
                    } catch (Exception cleanupError) {
                        logger.warn("Could not remove incomplete upload '{}': {}", targetFile.getName(), cleanupError.getMessage());
                    }
                }
            } else if (targetFile != null && !targetFile.getAbsolutePath().equals(storageKey)) {
                try {
                    Files.deleteIfExists(targetFile.toPath());
                } catch (Exception cleanupError) {
                    logger.debug("Could not remove temporary extracted file '{}': {}", targetFile.getName(), cleanupError.getMessage());
                }
            }
        }
    }
}
