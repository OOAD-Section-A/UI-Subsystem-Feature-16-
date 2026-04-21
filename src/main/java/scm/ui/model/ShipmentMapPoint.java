package scm.ui.model;

public class ShipmentMapPoint {
    private String shipmentId;
    private String shipmentStatus;
    private String vehicleId;
    private String originAddress;
    private String destinationAddress;
    private double latitude;
    private double longitude;
    private boolean liveGpsAvailable;

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String v) { shipmentId = v; }

    public String getShipmentStatus() { return shipmentStatus; }
    public void setShipmentStatus(String v) { shipmentStatus = v; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String v) { vehicleId = v; }

    public String getOriginAddress() { return originAddress; }
    public void setOriginAddress(String v) { originAddress = v; }

    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String v) { destinationAddress = v; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double v) { latitude = v; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double v) { longitude = v; }

    public boolean isLiveGpsAvailable() { return liveGpsAvailable; }
    public void setLiveGpsAvailable(boolean v) { liveGpsAvailable = v; }
}
