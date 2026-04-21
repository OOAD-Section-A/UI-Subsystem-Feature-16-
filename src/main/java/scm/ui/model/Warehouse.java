package scm.ui.model;

public class Warehouse {
    private String warehouseId, warehouseName;
    private String location;
    private int capacity;
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String v) { warehouseId = v; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { warehouseName = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { location = v; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int v) { capacity = v; }
    @Override public String toString() { return warehouseName; }
}
