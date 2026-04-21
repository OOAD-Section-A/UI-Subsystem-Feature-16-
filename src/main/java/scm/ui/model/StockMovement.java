package scm.ui.model;

public class StockMovement {
    private String movementId, movementType, fromBin, toBin, productId, movementTs;
    private int movedQty;

    public String getMovementId() { return movementId; }
    public void setMovementId(String v) { movementId = v; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String v) { movementType = v; }
    public String getFromBin() { return fromBin; }
    public void setFromBin(String v) { fromBin = v; }
    public String getToBin() { return toBin; }
    public void setToBin(String v) { toBin = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { productId = v; }
    public int getMovedQty() { return movedQty; }
    public void setMovedQty(int v) { movedQty = v; }
    public String getMovementTs() { return movementTs; }
    public void setMovementTs(String v) { movementTs = v; }
}
