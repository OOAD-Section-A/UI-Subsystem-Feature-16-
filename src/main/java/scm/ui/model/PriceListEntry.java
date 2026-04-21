package scm.ui.model;

public class PriceListEntry {
    private String priceId, skuId, regionCode, channel, priceType, currencyCode, effectiveFrom, effectiveTo, status;
    private double basePrice, priceFloor;

    public String getPriceId() { return priceId; }
    public void setPriceId(String v) { priceId = v; }
    public String getSkuId() { return skuId; }
    public void setSkuId(String v) { skuId = v; }
    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String v) { regionCode = v; }
    public String getChannel() { return channel; }
    public void setChannel(String v) { channel = v; }
    public String getPriceType() { return priceType; }
    public void setPriceType(String v) { priceType = v; }
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double v) { basePrice = v; }
    public double getPriceFloor() { return priceFloor; }
    public void setPriceFloor(double v) { priceFloor = v; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String v) { currencyCode = v; }
    public String getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(String v) { effectiveFrom = v; }
    public String getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(String v) { effectiveTo = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
}
