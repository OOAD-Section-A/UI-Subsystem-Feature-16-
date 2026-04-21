package scm.ui.model;

public class Order {
    private String orderId, customerId, orderStatus, orderDate, paymentStatus, salesChannel;
    private double totalAmount;

    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { orderId = v; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { customerId = v; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String v) { orderStatus = v; }
    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String v) { orderDate = v; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double v) { totalAmount = v; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String v) { paymentStatus = v; }
    public String getSalesChannel() { return salesChannel; }
    public void setSalesChannel(String v) { salesChannel = v; }
    @Override public String toString() { return orderId + " — " + customerId + " (" + orderStatus + ")"; }
}
