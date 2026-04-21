package scm.ui.model;

public class AuditLog {
    private int auditId;
    private String auditTimestamp, actionUser, actionDescription, moduleName;

    public int getAuditId() { return auditId; }
    public void setAuditId(int v) { auditId = v; }
    public String getAuditTimestamp() { return auditTimestamp; }
    public void setAuditTimestamp(String v) { auditTimestamp = v; }
    public String getActionUser() { return actionUser; }
    public void setActionUser(String v) { actionUser = v; }
    public String getActionDescription() { return actionDescription; }
    public void setActionDescription(String v) { actionDescription = v; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String v) { moduleName = v; }

    // Compatibility accessors used by UI panels
    public int getLogId() { return auditId; }
    public void setLogId(int v) { auditId = v; }
    public String getLogTimestamp() { return auditTimestamp; }
    public void setLogTimestamp(String v) { auditTimestamp = v; }
}
