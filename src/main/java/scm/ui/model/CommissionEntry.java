package scm.ui.model;

public class CommissionEntry {
    private String commissionId, agentId, periodStart, periodEnd, calculatedAt;
    private double totalSales, totalCommission;

    public String getCommissionId() { return commissionId; }
    public void setCommissionId(String v) { commissionId = v; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String v) { agentId = v; }
    public String getPeriodStart() { return periodStart; }
    public void setPeriodStart(String v) { periodStart = v; }
    public String getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(String v) { periodEnd = v; }
    public double getTotalSales() { return totalSales; }
    public void setTotalSales(double v) { totalSales = v; }
    public double getTotalCommission() { return totalCommission; }
    public void setTotalCommission(double v) { totalCommission = v; }
    public String getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(String v) { calculatedAt = v; }
}
