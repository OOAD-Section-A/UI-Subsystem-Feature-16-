package scm.ui.model;

public class DeliveryOrder {
    private String deliveryId, orderId, customerId, deliveryAddress, deliveryStatus, deliveryType, assignedAgent, warehouseId, createdAt;
    private double deliveryCost;

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String v) { deliveryId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { orderId = v; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { customerId = v; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String v) { deliveryAddress = v; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String v) { deliveryStatus = v; }
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String v) { deliveryType = v; }
    public double getDeliveryCost() { return deliveryCost; }
    public void setDeliveryCost(double v) { deliveryCost = v; }
    public String getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(String v) { assignedAgent = v; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String v) { warehouseId = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { createdAt = v; }
}
