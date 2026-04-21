package scm.ui.model;

public class BarcodeEvent {
    private String eventId, productId, eventType, sourceDevice, warehouseId, eventTimestamp, rawPayload;
    private int quantity;
    private String locationId;
    public String getEventId() { return eventId; }
    public void setEventId(String v) { eventId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { productId = v; }
    public String getEventType() { return eventType; }
    public void setEventType(String v) { eventType = v; }
    public String getSourceDevice() { return sourceDevice; }
    public void setSourceDevice(String v) { sourceDevice = v; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String v) { warehouseId = v; }
    public String getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(String v) { eventTimestamp = v; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String v) { rawPayload = v; }

    public String getSku() { return productId; }
    public void setSku(String v) { productId = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { quantity = v; }
    public String getLocationId() { return locationId != null ? locationId : warehouseId; }
    public void setLocationId(String v) { locationId = v; warehouseId = v; }
    public String getTimestamp() { return eventTimestamp; }
    public void setTimestamp(String v) { eventTimestamp = v; }
}
