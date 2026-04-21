package scm.ui.model;

public class FulfillmentOrder {
    private String fulfillmentId, orderId, fulfillmentStatus, assignedWarehouse, priorityLevel, createdAt;

    public String getFulfillmentId() { return fulfillmentId; }
    public void setFulfillmentId(String v) { fulfillmentId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { orderId = v; }
    public String getFulfillmentStatus() { return fulfillmentStatus; }
    public void setFulfillmentStatus(String v) { fulfillmentStatus = v; }
    public String getAssignedWarehouse() { return assignedWarehouse; }
    public void setAssignedWarehouse(String v) { assignedWarehouse = v; }
    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String v) { priorityLevel = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { createdAt = v; }
}
