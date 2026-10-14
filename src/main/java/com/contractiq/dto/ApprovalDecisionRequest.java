package com.contractiq.dto;

public class ApprovalDecisionRequest {
    private String reviewerName = "Legal Counsel";
    private String comment;

    public ApprovalDecisionRequest() {}

    public ApprovalDecisionRequest(String reviewerName, String comment) {
        this.reviewerName = reviewerName;
        this.comment = comment;
    }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
