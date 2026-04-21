package scm.ui.model;

public class Bin {
    private String binId, zoneId, binStatus;
    private String warehouseId;
    private int binCapacity;
    public String getBinId() { return binId; }
    public void setBinId(String v) { binId = v; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String v) { zoneId = v; }
    public int getBinCapacity() { return binCapacity; }
    public void setBinCapacity(int v) { binCapacity = v; }
    public String getBinStatus() { return binStatus; }
    public void setBinStatus(String v) { binStatus = v; }

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String v) { warehouseId = v; }
    public String getZone() { return zoneId; }
    public void setZone(String v) { zoneId = v; }
    public int getCapacity() { return binCapacity; }
    public void setCapacity(int v) { binCapacity = v; }
}
