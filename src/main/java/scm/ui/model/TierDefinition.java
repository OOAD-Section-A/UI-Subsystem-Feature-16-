package scm.ui.model;

public class TierDefinition {
    private int tierId;
    private String tierName;
    private double minSpendThreshold, defaultDiscountPct;
    public int getTierId() { return tierId; }
    public void setTierId(int v) { tierId = v; }
    public String getTierName() { return tierName; }
    public void setTierName(String v) { tierName = v; }
    public double getMinSpendThreshold() { return minSpendThreshold; }
    public void setMinSpendThreshold(double v) { minSpendThreshold = v; }
    public double getDefaultDiscountPct() { return defaultDiscountPct; }
    public void setDefaultDiscountPct(double v) { defaultDiscountPct = v; }
    @Override public String toString() { return tierName; }
}
