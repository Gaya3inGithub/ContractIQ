package com.contractiq.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Contract {
    private Long id;
    private String fileName;
    private String storedFilePath;
    private Long fileSizeBytes;
    private String fileHashSha256;

    private String contractTitle;
    private String contractType;
    private String partyFirst;
    private String partySecond;

    private LocalDate effectiveDate;
    private LocalDate expirationDate;
    private Boolean autoRenewal = false;
    private Integer renewalNoticePeriodDays;
    private LocalDate renewalNoticeDeadline;
    private String renewalTerms;

    private String paymentTerms;
    private String penaltiesClause;
    private String terminationConditions;

    private String extractionStatus = "PENDING"; // PENDING, EXTRACTING, EXTRACTED, FAILED
    private String reviewStatus = "NEEDS_REVIEW"; // NEEDS_REVIEW, PARTIALLY_APPROVED, APPROVED, REJECTED
    private BigDecimal confidenceScore = BigDecimal.ZERO;
    private String reviewNotes;
    private String rawTextExcerpt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<Obligation> obligations = new ArrayList<>();

    public Contract() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStoredFilePath() { return storedFilePath; }
    public void setStoredFilePath(String storedFilePath) { this.storedFilePath = storedFilePath; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getFileHashSha256() { return fileHashSha256; }
    public void setFileHashSha256(String fileHashSha256) { this.fileHashSha256 = fileHashSha256; }

    public String getContractTitle() { return contractTitle; }
    public void setContractTitle(String contractTitle) { this.contractTitle = contractTitle; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public String getPartyFirst() { return partyFirst; }
    public void setPartyFirst(String partyFirst) { this.partyFirst = partyFirst; }

    public String getPartySecond() { return partySecond; }
    public void setPartySecond(String partySecond) { this.partySecond = partySecond; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public Boolean getAutoRenewal() { return autoRenewal; }
    public void setAutoRenewal(Boolean autoRenewal) { this.autoRenewal = autoRenewal; }

    public Integer getRenewalNoticePeriodDays() { return renewalNoticePeriodDays; }
    public void setRenewalNoticePeriodDays(Integer renewalNoticePeriodDays) { this.renewalNoticePeriodDays = renewalNoticePeriodDays; }

    public LocalDate getRenewalNoticeDeadline() { return renewalNoticeDeadline; }
    public void setRenewalNoticeDeadline(LocalDate renewalNoticeDeadline) { this.renewalNoticeDeadline = renewalNoticeDeadline; }

    public String getRenewalTerms() { return renewalTerms; }
    public void setRenewalTerms(String renewalTerms) { this.renewalTerms = renewalTerms; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public String getPenaltiesClause() { return penaltiesClause; }
    public void setPenaltiesClause(String penaltiesClause) { this.penaltiesClause = penaltiesClause; }

    public String getTerminationConditions() { return terminationConditions; }
    public void setTerminationConditions(String terminationConditions) { this.terminationConditions = terminationConditions; }

    public String getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(String extractionStatus) { this.extractionStatus = extractionStatus; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public String getRawTextExcerpt() { return rawTextExcerpt; }
    public void setRawTextExcerpt(String rawTextExcerpt) { this.rawTextExcerpt = rawTextExcerpt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Obligation> getObligations() { return obligations; }
    public void setObligations(List<Obligation> obligations) { this.obligations = obligations; }
}
