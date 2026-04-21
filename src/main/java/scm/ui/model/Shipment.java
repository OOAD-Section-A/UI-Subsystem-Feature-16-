package scm.ui.model;

public class Shipment {
    private String shipmentId, orderId, originAddress, destinationAddress, shippingPriority, shipmentStatus, carrierId, trackingId;
    private double packageWeight, calculatedCost;

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String v) { shipmentId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { orderId = v; }
    public String getOriginAddress() { return originAddress; }
    public void setOriginAddress(String v) { originAddress = v; }
    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String v) { destinationAddress = v; }
    public double getPackageWeight() { return packageWeight; }
    public void setPackageWeight(double v) { packageWeight = v; }
    public String getShippingPriority() { return shippingPriority; }
    public void setShippingPriority(String v) { shippingPriority = v; }
    public String getShipmentStatus() { return shipmentStatus; }
    public void setShipmentStatus(String v) { shipmentStatus = v; }
    public String getCarrierId() { return carrierId; }
    public void setCarrierId(String v) { carrierId = v; }
    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String v) { trackingId = v; }
    public double getCalculatedCost() { return calculatedCost; }
    public void setCalculatedCost(double v) { calculatedCost = v; }
}
