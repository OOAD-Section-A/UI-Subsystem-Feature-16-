package scm.ui.model;

public class StockLevel {
    private String stockLevelId, productId;
    private int currentStockQty, reservedStockQty, availableStockQty, reorderThreshold, reorderQuantity, safetyStockLevel;

    public String getStockLevelId() { return stockLevelId; }
    public void setStockLevelId(String v) { stockLevelId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { productId = v; }
    public int getCurrentStockQty() { return currentStockQty; }
    public void setCurrentStockQty(int v) { currentStockQty = v; }
    public int getReservedStockQty() { return reservedStockQty; }
    public void setReservedStockQty(int v) { reservedStockQty = v; }
    public int getAvailableStockQty() { return availableStockQty; }
    public void setAvailableStockQty(int v) { availableStockQty = v; }
    public int getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(int v) { reorderThreshold = v; }
    public int getReorderQuantity() { return reorderQuantity; }
    public void setReorderQuantity(int v) { reorderQuantity = v; }
    public int getSafetyStockLevel() { return safetyStockLevel; }
    public void setSafetyStockLevel(int v) { safetyStockLevel = v; }
}
