package com.contractiq.dto;

import java.math.BigDecimal;

public class ObligationExtractionDto {
    private String obligationTitle;
    private String description;
    private String responsibleParty;
    private String deadlineDate; // String ISO YYYY-MM-DD or null
    private String frequency = "ONE_TIME";
    private String category = "OTHER";
    private String sourceExcerpt;
    private Integer pageNumber = 1;
    private BigDecimal confidence = new BigDecimal("0.85");

    public ObligationExtractionDto() {}

    public String getObligationTitle() { return obligationTitle; }
    public void setObligationTitle(String obligationTitle) { this.obligationTitle = obligationTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResponsibleParty() { return responsibleParty; }
    public void setResponsibleParty(String responsibleParty) { this.responsibleParty = responsibleParty; }

    public String getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(String deadlineDate) { this.deadlineDate = deadlineDate; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSourceExcerpt() { return sourceExcerpt; }
    public void setSourceExcerpt(String sourceExcerpt) { this.sourceExcerpt = sourceExcerpt; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
}
