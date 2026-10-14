package com.contractiq.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ContractExtractionDto {
    private String contractTitle;
    private String contractType;
    private String partyFirst;
    private String partySecond;
    private String effectiveDate;    // ISO YYYY-MM-DD or null
    private String expirationDate;   // ISO YYYY-MM-DD or null
    private Boolean autoRenewal = false;
    private Integer renewalNoticePeriodDays;
    private String renewalTerms;
    private String paymentTerms;
    private String penaltiesClause;
    private String terminationConditions;
    private BigDecimal confidenceScore = new BigDecimal("0.90");
    private String notes;
    private List<ObligationExtractionDto> obligations = new ArrayList<>();

    public ContractExtractionDto() {}

    public String getContractTitle() { return contractTitle; }
    public void setContractTitle(String contractTitle) { this.contractTitle = contractTitle; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public String getPartyFirst() { return partyFirst; }
    public void setPartyFirst(String partyFirst) { this.partyFirst = partyFirst; }

    public String getPartySecond() { return partySecond; }
    public void setPartySecond(String partySecond) { this.partySecond = partySecond; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }

    public Boolean getAutoRenewal() { return autoRenewal; }
    public void setAutoRenewal(Boolean autoRenewal) { this.autoRenewal = autoRenewal; }

    public Integer getRenewalNoticePeriodDays() { return renewalNoticePeriodDays; }
    public void setRenewalNoticePeriodDays(Integer renewalNoticePeriodDays) { this.renewalNoticePeriodDays = renewalNoticePeriodDays; }

    public String getRenewalTerms() { return renewalTerms; }
    public void setRenewalTerms(String renewalTerms) { this.renewalTerms = renewalTerms; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public String getPenaltiesClause() { return penaltiesClause; }
    public void setPenaltiesClause(String penaltiesClause) { this.penaltiesClause = penaltiesClause; }

    public String getTerminationConditions() { return terminationConditions; }
    public void setTerminationConditions(String terminationConditions) { this.terminationConditions = terminationConditions; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<ObligationExtractionDto> getObligations() { return obligations; }
    public void setObligations(List<ObligationExtractionDto> obligations) { this.obligations = obligations; }
}
