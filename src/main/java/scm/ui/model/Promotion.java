package scm.ui.model;

public class Promotion {
    private String promoId, promoName, couponCode, discountType, startDate, endDate;
    private double discountValue, minCartValue;
    private int maxUses, currentUseCount;

    public String getPromoId() { return promoId; }
    public void setPromoId(String v) { promoId = v; }
    public String getPromoName() { return promoName; }
    public void setPromoName(String v) { promoName = v; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String v) { couponCode = v; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String v) { discountType = v; }
    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double v) { discountValue = v; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String v) { startDate = v; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String v) { endDate = v; }
    public double getMinCartValue() { return minCartValue; }
    public void setMinCartValue(double v) { minCartValue = v; }
    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int v) { maxUses = v; }
    public int getCurrentUseCount() { return currentUseCount; }
    public void setCurrentUseCount(int v) { currentUseCount = v; }
}
