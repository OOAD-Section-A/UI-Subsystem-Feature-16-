package scm.ui.model;

public class StockRecord {
    private String stockId, productId, binId, lastUpdated;
    private int quantity;

    public String getStockId() { return stockId; }
    public void setStockId(String v) { stockId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { productId = v; }
    public String getBinId() { return binId; }
    public void setBinId(String v) { binId = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { quantity = v; }
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String v) { lastUpdated = v; }
}
