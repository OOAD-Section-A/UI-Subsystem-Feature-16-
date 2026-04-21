package scm.ui.model;

public class Product {
    private String productId, productName, sku, category, subCategory, supplierId, unitOfMeasure, storageConditions;
    private int shelfLifeDays;

    public String getProductId() { return productId; }
    public void setProductId(String v) { productId = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { productName = v; }
    public String getSku() { return sku; }
    public void setSku(String v) { sku = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String v) { subCategory = v; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String v) { supplierId = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { unitOfMeasure = v; }
    public String getStorageConditions() { return storageConditions; }
    public void setStorageConditions(String v) { storageConditions = v; }
    public int getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(int v) { shelfLifeDays = v; }
    @Override public String toString() { return productName + " [" + sku + "]"; }
}
