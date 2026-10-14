package com.contractiq.ai;

import com.contractiq.dto.ContractExtractionDto;

public class LlmExtractionResult {
    private final ContractExtractionDto contractData;
    private final String modelUsed;
    private final boolean realLlm;
    private final long executionTimeMs;
    private final String notes;

    public LlmExtractionResult(ContractExtractionDto contractData, String modelUsed, boolean realLlm, long executionTimeMs, String notes) {
        this.contractData = contractData;
        this.modelUsed = modelUsed;
        this.realLlm = realLlm;
        this.executionTimeMs = executionTimeMs;
        this.notes = notes;
    }

    public ContractExtractionDto getContractData() { return contractData; }
    public String getModelUsed() { return modelUsed; }
    public boolean isRealLlm() { return realLlm; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public String getNotes() { return notes; }
}
