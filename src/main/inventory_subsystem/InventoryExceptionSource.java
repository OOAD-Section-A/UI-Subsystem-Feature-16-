package inventory_subsystem;

import com.scm.core.SCMException;
import com.scm.factory.SCMExceptionFactory;
import com.scm.handler.SCMExceptionHandler;
import com.scm.subsystems.InventorySubsystem;
import scm.ui.exceptions.ExceptionUiLogger;

public class InventoryExceptionSource {

    private static final String SUBSYSTEM = "Inventory";
    private final InventorySubsystem exceptions = InventorySubsystem.INSTANCE;

    // Backward-compatible no-op: inventory exceptions are now self-routed by v3 handler.
    public void registerHandler(Object ignored) { }

    private void fireUnregistered(String detail) {
        ExceptionUiLogger.logToUiTable("Inventory", 0, "MINOR", detail, "SYSTEM");
        SCMException ex = SCMExceptionFactory.createUnregistered(SUBSYSTEM, detail);
        SCMExceptionHandler.INSTANCE.handle(ex);
    }

    public void fireResourceNotFound(int id, String type, String resourceId) {
        if (id == 166) {
            ExceptionUiLogger.logToUiTable("Inventory", 166, "MAJOR",
                    "ITEM_NOT_FOUND type=" + type + " resourceId=" + resourceId, resourceId);
            exceptions.onItemNotFound(resourceId);
            return;
        }
        fireUnregistered("fireResourceNotFound unknown id=" + id
                + " type=" + type + " resourceId=" + resourceId);
    }

    public void fireResourceExhausted(int id, String type,
                                      String resourceId,
                                      int requested, int available) {
        if (id == 167) {
            ExceptionUiLogger.logToUiTable("Inventory", 167, "MAJOR",
                    "INSUFFICIENT_STOCK type=" + type + " resourceId=" + resourceId
                            + " requested=" + requested + " available=" + available,
                    resourceId);
            exceptions.onInsufficientStock(resourceId, requested, available);
            return;
        }
        fireUnregistered("fireResourceExhausted unknown id=" + id
                + " type=" + type + " resourceId=" + resourceId
                + " requested=" + requested + " available=" + available);
    }

    public void fireConflict(int id, String entityType,
                             String entityId, String reason) {
        switch (id) {
            case 110 -> {
                ExceptionUiLogger.logToUiTable("Inventory", 110, "WARNING",
                        "STOCK_UPDATE_CONFLICT entityType=" + entityType + " entityId=" + entityId
                                + " reason=" + reason,
                        entityId);
                exceptions.onStockUpdateConflict(entityId);
            }
            case 318 -> {
                ExceptionUiLogger.logToUiTable("Inventory", 318, "MINOR",
                        "DUPLICATE_SKU entityType=" + entityType + " entityId=" + entityId
                                + " reason=" + reason,
                        entityId);
                exceptions.onDuplicateSku(entityId);
            }
            default -> fireUnregistered("fireConflict unknown id=" + id
                    + " entityType=" + entityType + " entityId=" + entityId
                    + " reason=" + reason);
        }
    }
}
