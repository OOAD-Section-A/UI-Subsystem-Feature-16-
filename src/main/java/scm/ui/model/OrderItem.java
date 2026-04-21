package scm.ui.model;

public class OrderItem {
    private String orderItemId, orderId, productId;
    private int orderedQuantity;
    private double unitPrice, lineTotal;

    public String getOrderItemId() { return orderItemId; }
    public void setOrderItemId(String v) { orderItemId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { orderId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { productId = v; }
    public int getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(int v) { orderedQuantity = v; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double v) { unitPrice = v; }
    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double v) { lineTotal = v; }
}
