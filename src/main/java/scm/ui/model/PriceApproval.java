package scm.ui.model;

public class PriceApproval {
    private String approvalId, requestType, requestedBy, justificationText, approvingManagerId, approvalStatus, createdAt;
    private double requestedDiscountAmt;

    public String getApprovalId() { return approvalId; }
    public void setApprovalId(String v) { approvalId = v; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String v) { requestType = v; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String v) { requestedBy = v; }
    public double getRequestedDiscountAmt() { return requestedDiscountAmt; }
    public void setRequestedDiscountAmt(double v) { requestedDiscountAmt = v; }
    public String getJustificationText() { return justificationText; }
    public void setJustificationText(String v) { justificationText = v; }
    public String getApprovingManagerId() { return approvingManagerId; }
    public void setApprovingManagerId(String v) { approvingManagerId = v; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String v) { approvalStatus = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { createdAt = v; }
}
