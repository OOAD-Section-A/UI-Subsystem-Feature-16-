package scm.ui.model;

public class WarehouseProductStock {
    private String warehouseId;
    private String productId;
    private String skuId;
    private String binId;
    private String productName;
    private int quantity;

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String v) { warehouseId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { productId = v; }
    public String getSkuId() { return skuId; }
    public void setSkuId(String v) { skuId = v; }
    public String getBinId() { return binId; }
    public void setBinId(String v) { binId = v; }
    public String getProductName() { return productName != null ? productName : skuId; }
    public void setProductName(String v) { productName = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { quantity = v; }
}
