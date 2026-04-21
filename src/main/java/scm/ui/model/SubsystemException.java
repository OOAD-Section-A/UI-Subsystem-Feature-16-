package scm.ui.model;

public class SubsystemException {
    private String exceptionId, subsystemName, referenceId, severity, exceptionMessage, status, createdAt, resolvedAt;
    public String getExceptionId() { return exceptionId; }
    public void setExceptionId(String v) { exceptionId = v; }
    public String getSubsystemName() { return subsystemName; }
    public void setSubsystemName(String v) { subsystemName = v; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String v) { referenceId = v; }
    public String getSeverity() { return severity; }
    public void setSeverity(String v) { severity = v; }
    public String getExceptionMessage() { return exceptionMessage; }
    public void setExceptionMessage(String v) { exceptionMessage = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { createdAt = v; }
    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String v) { resolvedAt = v; }

    // Compatibility accessors used by UI panels
    public String getSubsystemCode() { return subsystemName; }
    public void setSubsystemCode(String v) { subsystemName = v; }
    public String getSeverityLevel() { return severity; }
    public void setSeverityLevel(String v) { severity = v; }
    public String getRaisedBy() { return referenceId; }
    public void setRaisedBy(String v) { referenceId = v; }
}
