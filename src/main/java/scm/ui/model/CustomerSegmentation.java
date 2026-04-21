package scm.ui.model;

public class CustomerSegmentation {
    private String segmentationId, customerId;
    private double cumulativeSpend;
    private int assignedTierId, overrideTierId;
    private boolean manualOverride;
    public String getSegmentationId() { return segmentationId; }
    public void setSegmentationId(String v) { segmentationId = v; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { customerId = v; }
    public double getCumulativeSpend() { return cumulativeSpend; }
    public void setCumulativeSpend(double v) { cumulativeSpend = v; }
    public int getAssignedTierId() { return assignedTierId; }
    public void setAssignedTierId(int v) { assignedTierId = v; }
    public boolean isManualOverride() { return manualOverride; }
    public void setManualOverride(boolean v) { manualOverride = v; }
    public int getOverrideTierId() { return overrideTierId; }
    public void setOverrideTierId(int v) { overrideTierId = v; }
}
