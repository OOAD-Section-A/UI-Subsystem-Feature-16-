package scm.ui.model;

public class DiscountPolicy {
    private String policyId, policyName, stackingRule;
    private int priorityLevel, perishabilityDays;
    private double maxDiscountCapPct, clearanceDiscountPct;

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String v) { policyId = v; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String v) { policyName = v; }
    public String getStackingRule() { return stackingRule; }
    public void setStackingRule(String v) { stackingRule = v; }
    public int getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(int v) { priorityLevel = v; }
    public double getMaxDiscountCapPct() { return maxDiscountCapPct; }
    public void setMaxDiscountCapPct(double v) { maxDiscountCapPct = v; }
    public int getPerishabilityDays() { return perishabilityDays; }
    public void setPerishabilityDays(int v) { perishabilityDays = v; }
    public double getClearanceDiscountPct() { return clearanceDiscountPct; }
    public void setClearanceDiscountPct(double v) { clearanceDiscountPct = v; }
}
