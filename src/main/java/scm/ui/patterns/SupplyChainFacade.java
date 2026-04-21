package scm.ui.patterns;

import inventory_subsystem.InventoryExceptionSource;
import inventory_subsystem.InventoryBatch;
import inventory_subsystem.InventoryItem;
import inventory_subsystem.InventoryRepository;
import inventory_subsystem.InventoryService;
import inventory_subsystem.InventoryUI;
import inventory_subsystem.IssuingPolicy;
import inventory_subsystem.StockTransaction;
import inventory_subsystem.Supplier;
import com.ramennoodles.delivery.facade.DeliveryMonitoringFacadeDB;
import com.ramennoodles.delivery.observer.DeliveryEventListener;
import com.ramennoodles.delivery.observer.DeliveryEventType;
import scm.ui.db.DAOFactory;
import scm.ui.db.DatabaseConnectionPool;
import scm.ui.db.dao.*;
import scm.ui.model.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * STRUCTURAL PATTERN — FACADE
 * SupplyChainFacade is the single entry point for all service operations.
 * Referenced in schema.sql pattern notes.
 * UI panels call facade methods; they never touch DAOs directly.
 *
 * INVENTORY SUBSYSTEM INTEGRATION — UPDATED (Integration Spec UPDATED):
 *   Setup  : new InventoryService(exceptionSource)
 *              — repository is now created internally by InventoryService.
 *   Access : via InventoryUI — addStock, removeStock, transferStock, getStock
 *              — ALL operations now carry supplierId as the third parameter.
 *   Rules  : UI must not implement business logic; do not access repository directly.
 *
 * Changes from previous version:
 *   1. InventoryService constructor: was (repository, exceptionSource)
 *                                    now (exceptionSource) only.
 *   2. InventoryUI method signatures: supplierId added to all four methods.
 *   3. Facade inventory methods:      supplierId param added; passed through
 *                                     to inventoryService calls.
 *   4. getRepoQuantity / getQuantityAtLocation: supplierId param added.
 *   5. ensureInventoryRepositoryHydrated: seeds InventoryItem with supplierId
 *                                         resolved from DB supplier linkage.
 */
public class SupplyChainFacade {

    private static volatile SupplyChainFacade instance;

    private final DAOFactory daoFactory;
    private final EventBus   eventBus;

    // ── Inventory Subsystem (Integration Spec §2 — Setup, UPDATED) ───────────
    // InventoryService now owns its own InventoryRepository internally.
    // The facade only holds the InventoryExceptionSource so the central
    // SCMExceptionHandler can be registered at startup (SCMApplication §8).
    private final InventoryExceptionSource inventoryExceptionSource;
    private final InventoryUI              inventoryService;
    private volatile boolean               inventoryRepositoryHydrated;
    private volatile boolean               inventoryMetadataTablesReady;

    // ── Real-Time Delivery Monitoring Subsystem (Team Ramen Noodles #9) ──────
    private DeliveryMonitoringFacadeDB deliverySystem;

    private UIUser currentUser;

    private SupplyChainFacade() {
        this.daoFactory = new DAOFactory(DatabaseConnectionPool.getInstance());
        this.eventBus   = EventBus.getInstance();

        // Updated constructor — InventoryService creates its own repository
        // (InventoryRepository) internally, wired to its own SupplyChainDatabaseFacade.
        this.inventoryExceptionSource = new InventoryExceptionSource();
        this.inventoryService         = new InventoryService(inventoryExceptionSource);

        // ── Delivery Monitoring ───────────────────────────────────────────
        try {
            this.deliverySystem = new DeliveryMonitoringFacadeDB();
            wireDeliveryEventsToEventBus();
            System.out.println("[SCM] Real-Time Delivery Monitoring system initialised.");
        } catch (Exception e) {
            System.out.println("[SCM] Delivery Monitoring system unavailable: " + e.getMessage());
            this.deliverySystem = null;
        }
    }

    public static SupplyChainFacade getInstance() {
        if (instance == null) {
            synchronized (SupplyChainFacade.class) {
                if (instance == null) instance = new SupplyChainFacade();
            }
        }
        return instance;
    }

    /**
     * Provides the InventoryExceptionSource so SCMApplication can register
     * the central SCMExceptionHandler against it at startup.
     * Per Integration Spec §8 — exception handler must be registered externally.
     */
    public InventoryExceptionSource getInventoryExceptionSource() {
        return inventoryExceptionSource;
    }

    // ── Delivery Monitoring — event bridge & public access ───────────────────

    /**
     * Bridges every DeliveryMonitoringFacadeDB event into the UI EventBus so that
     * any panel or badge callback can react without coupling to the delivery JAR.
     */
    private void wireDeliveryEventsToEventBus() {
        if (deliverySystem == null) return;

        DeliveryEventListener statusBridge = (type, data) ->
            javax.swing.SwingUtilities.invokeLater(() ->
                eventBus.publish(EventBus.Event.DELIVERY_STATUS_CHANGED, data));

        DeliveryEventListener gpsBridge = (type, data) ->
            javax.swing.SwingUtilities.invokeLater(() ->
                eventBus.publish(EventBus.Event.DELIVERY_GPS_UPDATED, data));

        DeliveryEventListener podBridge = (type, data) ->
            javax.swing.SwingUtilities.invokeLater(() ->
                eventBus.publish(EventBus.Event.DELIVERY_POD_SUBMITTED, data));

        DeliveryEventListener etaBridge = (type, data) ->
            javax.swing.SwingUtilities.invokeLater(() ->
                eventBus.publish(EventBus.Event.DELIVERY_ETA_UPDATED, data));

        deliverySystem.subscribeToEvents(DeliveryEventType.ORDER_CREATED,    statusBridge);
        deliverySystem.subscribeToEvents(DeliveryEventType.RIDER_ASSIGNED,   statusBridge);
        deliverySystem.subscribeToEvents(DeliveryEventType.STATUS_CHANGED,   statusBridge);
        deliverySystem.subscribeToEvents(DeliveryEventType.ORDER_DELIVERED,  statusBridge);
        deliverySystem.subscribeToEvents(DeliveryEventType.ORDER_FAILED,     statusBridge);
        deliverySystem.subscribeToEvents(DeliveryEventType.GEOFENCE_ENTRY,   statusBridge);
        deliverySystem.subscribeToEvents(DeliveryEventType.GEOFENCE_EXIT,    statusBridge);
        deliverySystem.subscribeToEvents(DeliveryEventType.LOCATION_UPDATED, gpsBridge);
        deliverySystem.subscribeToEvents(DeliveryEventType.POD_SUBMITTED,    podBridge);
        deliverySystem.subscribeToEvents(DeliveryEventType.ETA_UPDATED,      etaBridge);
    }

    /**
     * Returns the DeliveryMonitoringFacadeDB for use by DeliveryMonitoringPanel.
     * Returns null if the delivery JAR was not found at startup.
     */
    public DeliveryMonitoringFacadeDB getDeliverySystem() {
        return deliverySystem;
    }

    /**
     * Convenience wrapper: creates a SYSTEM notification via the facade's
     * notification DAO, called by DeliveryMonitoringPanel on key events.
     */
    public long addSystemNotification(String type, String message) {
        return addNotification(type, message);
    }

    // ── Session ─────────────────────────────────────────────────────────────

    public UIUser login(String username, String passwordHash) {
        UserDAO dao = daoFactory.getDAO(DAOFactory.DAOType.USER);
        UIUser user = dao.findByUsernameAndPassword(username, passwordHash);
        if (user != null && !user.isAccountLocked()) {
            dao.updateLastLogin(user.getUserId());
            this.currentUser = user;
            audit("LOGIN", username, "C-01");
            eventBus.publish(EventBus.Event.USER_LOGGED_IN, user);
        } else if (user != null) {
            dao.incrementLoginAttempts(user.getUserId());
            if (user.getLoginAttemptCount() + 1 >= 5) {
                dao.lockAccount(user.getUserId(), true);
            }
        }
        return user;
    }

    public void logout() {
        if (currentUser != null) {
            audit("LOGOUT", currentUser.getUsername(), "C-01");
            eventBus.publish(EventBus.Event.USER_LOGGED_OUT, currentUser);
            currentUser = null;
        }
    }

    public UIUser getCurrentUser() { return currentUser; }

    // ── Audit ────────────────────────────────────────────────────────────────

    public void audit(String action, String description, String module) {
        AuditLogDAO dao = daoFactory.getDAO(DAOFactory.DAOType.AUDIT_LOG);
        AuditLog log = new AuditLog();
        log.setActionUser(currentUser != null ? currentUser.getUsername() : "SYSTEM");
        log.setActionDescription(action + " | " + description);
        log.setModuleName(module);
        try { dao.insert(log); } catch (Exception ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INVENTORY SUBSYSTEM OPERATIONS  (Integration Spec UPDATED — §3 / §4)
    //
    //  ALL four operations now include supplierId as the third parameter,
    //  matching the updated InventoryUI interface:
    //      addStock      (sku, locationId, supplierId, quantity)
    //      removeStock   (sku, locationId, supplierId, quantity)
    //      transferStock (sku, fromLocation, toLocation, supplierId, quantity)
    //      getStock      (sku, locationId, supplierId)
    //
    //  The facade:
    //    • Does NOT access inventoryRepository directly
    //    • Does NOT modify InventoryItem
    //    • Does NOT handle exceptions — they are fired to exceptionSource
    //    • Publishes STOCK_CHANGED to EventBus after mutating operations
    //    • Writes an audit trail entry for every operation
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Add stock to a SKU or Product ID at a location for a given supplier.
     * Integration Spec §4.1 — Used by: Warehouse Management, Barcode/RFID.
     * Behavior: creates batch, stores supplier link, updates cost-ready structure.
     *
     * @param sku        product identifier (SKU or Product ID)
     * @param locationId bin ID or warehouse ID
     * @param supplierId supplier ID (required — links batch to supplier)
     * @param quantity   amount to add (must be > 0)
     */
    public void inventoryAddStock(String sku, String locationId,
                                  String supplierId, int quantity) {
        ensureInventoryRepositoryHydrated();
        applyDbIssuingPolicyToRuntime();
        if (quantity <= 0 || isBlank(sku) || isBlank(locationId) || isBlank(supplierId)) {
            return;
        }

        Product product = ensureProductForStockOperation(sku, supplierId);
        String inventorySku = canonicalInventorySku(sku, product);
        String effectiveSupplierId = resolveSupplierForProduct(product, supplierId);
        String resolvedBinId = resolveBinForLocation(locationId);
        if (isBlank(resolvedBinId)) {
            inventoryExceptionSource.fireResourceNotFound(166, "Bin/Warehouse", locationId);
            return;
        }

        int before = getRepoQuantity(inventorySku, resolvedBinId, effectiveSupplierId);
        inventoryService.addStock(inventorySku, resolvedBinId, effectiveSupplierId, quantity);
        int after = getRepoQuantity(inventorySku, resolvedBinId, effectiveSupplierId);
        int actualDelta = after - before;
        if (actualDelta <= 0) return;

        if (product != null) {
            if (!isBlank(resolvedBinId)) {
                applyStockRecordDelta(product.getProductId(), resolvedBinId, actualDelta);
                insertStockMovement("INBOUND", null, resolvedBinId, product.getProductId(), actualDelta);
            }
            syncStockLevelFromRecords(product.getProductId());
        }

        audit("INV_ADD_STOCK",
              "addStock sku=" + sku + " location=" + locationId
                      + " supplier=" + effectiveSupplierId + " qty=+" + actualDelta,
              "C-04");
        eventBus.publish(EventBus.Event.STOCK_CHANGED,
              "ADD " + sku + "@" + locationId + " sup=" + effectiveSupplierId + " +" + actualDelta);
    }

    /**
     * Remove stock from a SKU or Product ID at a location for a given supplier.
     * Integration Spec §4.2 — Used by: Order Fulfillment.
     * Behavior: FIFO/FEFO applied internally, batch quantities updated, transaction recorded.
     * Fires 166 (ITEM_NOT_FOUND), 167 (INSUFFICIENT_STOCK), 110 (STOCK_UPDATE_CONFLICT).
     *
     * @param sku        product identifier (SKU or Product ID)
     * @param locationId bin ID or warehouse ID
     * @param supplierId supplier ID
     * @param quantity   amount to remove (must be > 0)
     */
    public void inventoryRemoveStock(String sku, String locationId,
                                     String supplierId, int quantity) {
        ensureInventoryRepositoryHydrated();
        applyDbIssuingPolicyToRuntime();
        if (quantity <= 0 || isBlank(sku) || isBlank(locationId) || isBlank(supplierId)) {
            return;
        }

        Product product = ensureProductForStockOperation(sku, supplierId);
        String inventorySku = canonicalInventorySku(sku, product);
        String effectiveSupplierId = resolveSupplierForProduct(product, supplierId);
        String resolvedBinId = resolveBinForLocation(locationId);
        if (isBlank(resolvedBinId)) {
            inventoryExceptionSource.fireResourceNotFound(166, "Bin/Warehouse", locationId);
            return;
        }

        int before = getRepoQuantity(inventorySku, resolvedBinId, effectiveSupplierId);
        inventoryService.removeStock(inventorySku, resolvedBinId, effectiveSupplierId, quantity);
        int after = getRepoQuantity(inventorySku, resolvedBinId, effectiveSupplierId);
        int actualDelta = before - after;
        if (actualDelta <= 0) return;

        if (product != null) {
            if (!isBlank(resolvedBinId)) {
                applyStockRecordDelta(product.getProductId(), resolvedBinId, -actualDelta);
                insertStockMovement("OUTBOUND", resolvedBinId, null, product.getProductId(), actualDelta);
            }
            syncStockLevelFromRecords(product.getProductId());
        }

        audit("INV_REMOVE_STOCK",
              "removeStock sku=" + sku + " location=" + locationId
                      + " supplier=" + effectiveSupplierId + " qty=-" + actualDelta,
              "C-04");
        eventBus.publish(EventBus.Event.STOCK_CHANGED,
              "REMOVE " + sku + "@" + locationId + " sup=" + effectiveSupplierId + " -" + actualDelta);
    }

    /**
     * Transfer stock between locations for a given supplier.
     * Integration Spec §4.3 — Used by: Warehouse Management.
     * Behavior: removes from source (traceability maintained), creates batch at destination.
     * Fires 166 / 167 on failure.
     *
     * @param sku          product identifier (SKU or Product ID)
     * @param fromLocation source bin ID or warehouse ID
     * @param toLocation   destination bin ID or warehouse ID
     * @param supplierId   supplier ID (maintained on both sides)
     * @param quantity     amount to transfer (must be > 0)
     */
    public void inventoryTransferStock(String sku, String fromLocation,
                                       String toLocation, String supplierId, int quantity) {
        ensureInventoryRepositoryHydrated();
        applyDbIssuingPolicyToRuntime();
        if (quantity <= 0 || isBlank(sku) || isBlank(fromLocation)
                || isBlank(toLocation) || isBlank(supplierId)) {
            return;
        }

        Product product = ensureProductForStockOperation(sku, supplierId);
        String inventorySku = canonicalInventorySku(sku, product);
        String effectiveSupplierId = resolveSupplierForProduct(product, supplierId);
        String resolvedFromBinId = resolveBinForLocation(fromLocation);
        String resolvedToBinId   = resolveBinForLocation(toLocation);
        if (isBlank(resolvedFromBinId)) {
            inventoryExceptionSource.fireResourceNotFound(166, "From Bin/Warehouse", fromLocation);
            return;
        }
        if (isBlank(resolvedToBinId)) {
            inventoryExceptionSource.fireResourceNotFound(166, "To Bin/Warehouse", toLocation);
            return;
        }

        int beforeFrom = getRepoQuantity(inventorySku, resolvedFromBinId, effectiveSupplierId);
        int beforeTo   = getRepoQuantity(inventorySku, resolvedToBinId,   effectiveSupplierId);
        inventoryService.transferStock(inventorySku, resolvedFromBinId, resolvedToBinId, effectiveSupplierId, quantity);
        int afterFrom  = getRepoQuantity(inventorySku, resolvedFromBinId, effectiveSupplierId);
        int afterTo    = getRepoQuantity(inventorySku, resolvedToBinId,   effectiveSupplierId);

        int movedOut = beforeFrom - afterFrom;
        int movedIn  = afterTo   - beforeTo;
        if (movedOut <= 0 || movedIn <= 0) return;

        int movedQty = Math.min(movedOut, movedIn);
        if (product != null) {
            if (!isBlank(resolvedFromBinId)) {
                applyStockRecordDelta(product.getProductId(), resolvedFromBinId, -movedQty);
            }
            if (!isBlank(resolvedToBinId)) {
                applyStockRecordDelta(product.getProductId(), resolvedToBinId, movedQty);
            }
            insertStockMovement("TRANSFER", resolvedFromBinId, resolvedToBinId,
                                product.getProductId(), movedQty);
            syncStockLevelFromRecords(product.getProductId());
        }

        audit("INV_TRANSFER_STOCK",
              "transferStock sku=" + sku + " from=" + fromLocation
                      + " to=" + toLocation + " supplier=" + effectiveSupplierId
                      + " qty=" + movedQty,
              "C-04");
        eventBus.publish(EventBus.Event.STOCK_CHANGED,
              "TRANSFER " + sku + " " + fromLocation + "->" + toLocation
                      + " sup=" + effectiveSupplierId);
    }

    /**
     * Get current stock quantity for a SKU or Product ID at a location for a given supplier.
     * Integration Spec §4.4 — Used by: Order Fulfillment, Reporting and Analytics Dashboard.
     * Fires 166 (ITEM_NOT_FOUND) and returns 0 if not found.
     *
     * When supplierId is blank or "ALL", sums across all suppliers at that location.
     *
     * @param sku        product identifier (SKU or Product ID)
     * @param locationId bin ID or warehouse ID
     * @param supplierId supplier ID, or blank/"ALL" to sum all suppliers
     * @return current quantity, or 0 if not found
     */
    public int inventoryGetStock(String sku, String locationId, String supplierId) {
        ensureInventoryRepositoryHydrated();
        if (isBlank(sku) || isBlank(locationId)) return 0;

        Product product = findProductByIdentifier(sku);
        if (product == null) return 0;
        String productId = product.getProductId();
        if (isBlank(productId)) return 0;

        String normalizedSupplier = normalizeSupplierForRead(product, supplierId);
        boolean allSuppliers = "ALL".equalsIgnoreCase(normalizedSupplier);

        // Warehouse-level query: sum across all bins in the warehouse
        String warehouseId = findWarehouseIdForLocation(locationId);
        if (!isBlank(warehouseId)) {
            return quantityFromDbForWarehouse(productId, warehouseId);
        }

        String resolvedBinId = resolveBinForLocation(locationId);
        if (!isBlank(resolvedBinId)) {
            return quantityFromDbForBin(productId, resolvedBinId);
        }

        return 0;
    }

    public void inventoryAddSupplier(String supplierId, String name,
                                     int leadTimeDays, double performanceRating) {
        if (isBlank(supplierId) || isBlank(name)) return;
        String supplierKey = supplierId.trim();
        ensureInventoryMetadataTables();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO inventory_suppliers(supplier_id, name, lead_time_days, performance_rating) " +
                             "VALUES(?,?,?,?) " +
                             "ON DUPLICATE KEY UPDATE name=VALUES(name), lead_time_days=VALUES(lead_time_days), " +
                             "performance_rating=VALUES(performance_rating), updated_at=CURRENT_TIMESTAMP"
             )) {
            ps.setString(1, supplierKey);
            ps.setString(2, name.trim());
            ps.setInt(3, Math.max(0, leadTimeDays));
            ps.setDouble(4, Math.max(0.0, performanceRating));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Inventory] inventoryAddSupplier DB error: " + e.getMessage());
        }
    }

    public Supplier inventoryGetSupplier(String supplierId) {
        if (isBlank(supplierId)) return null;
        String supplierKey = supplierId.trim();
        ensureInventoryMetadataTables();

        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT supplier_id, name, lead_time_days, performance_rating " +
                             "FROM inventory_suppliers WHERE supplier_id=?"
             )) {
            ps.setString(1, supplierKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Supplier(
                            rs.getString("supplier_id"),
                            rs.getString("name"),
                            rs.getInt("lead_time_days"),
                            rs.getDouble("performance_rating")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[Inventory] inventoryGetSupplier DB error: " + e.getMessage());
        }

        // Fallback: derive from existing product linkage if supplier exists in seeded DB data
        for (Product p : getAllProducts()) {
            if (p != null && !isBlank(p.getSupplierId())
                    && supplierKey.equalsIgnoreCase(p.getSupplierId().trim())) {
                return new Supplier(supplierKey, supplierKey, 0, 0.0);
            }
        }
        return null;
    }

    public int inventoryGetTotalStockBySupplier(String supplierId) {
        if (isBlank(supplierId)) return 0;
        String normalizedSupplierId = supplierId.trim();
        StockLevelDAO stockLevelDAO = daoFactory.getDAO(DAOFactory.DAOType.STOCK_LEVEL);

        int total = 0;
        for (Product product : getAllProducts()) {
            if (product == null || isBlank(product.getSupplierId()) || isBlank(product.getProductId())) continue;
            if (!normalizedSupplierId.equalsIgnoreCase(product.getSupplierId().trim())) continue;

            int fromRecords = quantityFromDbForProduct(product.getProductId().trim());
            if (fromRecords > 0) {
                total += fromRecords;
                continue;
            }

            StockLevel level = stockLevelDAO.findByProduct(product.getProductId().trim());
            if (level == null && !isBlank(product.getSku())) {
                level = stockLevelDAO.findByProduct(product.getSku().trim());
            }
            if (level != null) {
                total += Math.max(0, level.getCurrentStockQty());
            }
        }
        return total;
    }

    public String inventoryGetIssuingPolicy() {
        ensureInventoryMetadataTables();
        String storedPolicy = readInventorySetting("ISSUING_POLICY");
        if (isBlank(storedPolicy)) {
            storedPolicy = IssuingPolicy.FIFO.name();
        }
        String normalized = storedPolicy.trim().toUpperCase(Locale.ROOT);
        if (!"FIFO".equals(normalized) && !"FEFO".equals(normalized)) {
            normalized = IssuingPolicy.FIFO.name();
        }
        if (inventoryService instanceof InventoryService concrete) {
            concrete.setIssuingPolicy(IssuingPolicy.valueOf(normalized));
        }
        return normalized;
    }

    public void inventorySetIssuingPolicy(String issuingPolicy) {
        if (isBlank(issuingPolicy)) return;
        ensureInventoryMetadataTables();
        String normalized = issuingPolicy.trim().toUpperCase(Locale.ROOT);
        if (!"FIFO".equals(normalized) && !"FEFO".equals(normalized)) {
            inventoryExceptionSource.fireConflict(168, "IssuingPolicy", issuingPolicy, "Unsupported policy");
            return;
        }
        writeInventorySetting("ISSUING_POLICY", normalized);
        if (!(inventoryService instanceof InventoryService concrete)) return;
        try {
            concrete.setIssuingPolicy(IssuingPolicy.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            inventoryExceptionSource.fireConflict(168, "IssuingPolicy", issuingPolicy, "Unsupported policy");
        }
    }

    public List<InventoryBatch> inventoryGetBatches(String skuOrProductId, String locationId, String supplierId) {
        if (isBlank(skuOrProductId) || isBlank(locationId)) return Collections.emptyList();
        Product product = findProductByIdentifier(skuOrProductId);
        if (product == null || isBlank(product.getProductId())) return Collections.emptyList();
        String effectiveSupplierId = resolveSupplierForProduct(product, supplierId);
        String resolvedBinId = resolveBinForLocation(locationId);
        String resolvedWarehouseId = findWarehouseIdForLocation(locationId);
        if (isBlank(resolvedBinId) && isBlank(resolvedWarehouseId)) return Collections.emptyList();

        int locationQty = 0;
        if (!isBlank(resolvedWarehouseId)) {
            locationQty = quantityFromDbForWarehouse(product.getProductId().trim(), resolvedWarehouseId);
        } else {
            locationQty = quantityFromDbForBin(product.getProductId().trim(), resolvedBinId);
        }
        if (locationQty <= 0) return Collections.emptyList();

        List<InventoryBatch> batches = loadBatchesFromDb(product.getProductId().trim(), effectiveSupplierId, locationQty);
        if (!batches.isEmpty()) return batches;

        return List.of(new InventoryBatch(
                "UNBATCHED-" + product.getProductId().trim(),
                locationQty,
                LocalDateTime.now(),
                null,
                new ArrayList<>(),
                0.0
        ));
    }

    public List<StockTransaction> inventoryGetTransactions() {
        List<StockTransaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT sm.movement_type, sm.from_bin, sm.to_bin, sm.moved_qty, sm.product_id, " +
                             "COALESCE(p.sku, sm.product_id) AS sku, COALESCE(p.supplier_id, 'DEFAULT') AS supplier_id " +
                             "FROM stock_movements sm " +
                             "LEFT JOIN products p ON p.product_id = sm.product_id OR p.sku = sm.product_id " +
                             "ORDER BY sm.movement_ts DESC LIMIT 500"
             );
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String movementType = rs.getString("movement_type");
                String fromBin = rs.getString("from_bin");
                String toBin = rs.getString("to_bin");
                int qty = Math.max(0, rs.getInt("moved_qty"));
                String sku = rs.getString("sku");
                String dbSupplierId = rs.getString("supplier_id");

                if ("TRANSFER".equalsIgnoreCase(movementType)) {
                    transactions.add(new StockTransaction(
                            sku,
                            fromBin,
                            dbSupplierId,
                            -qty,
                            "TRANSFER_OUT"
                    ));
                    transactions.add(new StockTransaction(
                            sku,
                            toBin,
                            dbSupplierId,
                            qty,
                            "TRANSFER_IN"
                    ));
                    continue;
                }

                boolean positive = "INBOUND".equalsIgnoreCase(movementType)
                        || "RETURN".equalsIgnoreCase(movementType);
                int delta = positive ? qty : -qty;
                String txType = switch (movementType == null ? "" : movementType.toUpperCase(Locale.ROOT)) {
                    case "INBOUND" -> "ADD";
                    case "OUTBOUND" -> "REMOVE";
                    default -> movementType;
                };

                String location = !isBlank(toBin) ? toBin : fromBin;
                transactions.add(new StockTransaction(
                        sku,
                        location,
                        dbSupplierId,
                        delta,
                        txType
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Inventory] inventoryGetTransactions DB error: " + e.getMessage());
        }
        return transactions;
    }

    // ── Products (C-04) ──────────────────────────────────────────────────────

    public List<Product> getAllProducts() {
        return ((ProductDAO) daoFactory.getDAO(DAOFactory.DAOType.PRODUCT)).findAll();
    }

    public int createProduct(Product p) {
        ProductDAO dao = daoFactory.getDAO(DAOFactory.DAOType.PRODUCT);
        int r = dao.insert(p);
        if (r > 0) ensureStockLevelExists(p.getProductId());
        audit("CREATE_PRODUCT", "Created product: " + p.getProductName(), "C-04");
        eventBus.publish(EventBus.Event.PRODUCT_CREATED, p);
        return r;
    }

    public int updateProduct(Product p) {
        ProductDAO dao = daoFactory.getDAO(DAOFactory.DAOType.PRODUCT);
        int r = dao.update(p);
        audit("UPDATE_PRODUCT", "Updated product: " + p.getProductName(), "C-04");
        eventBus.publish(EventBus.Event.PRODUCT_UPDATED, p);
        return r;
    }

    public int deleteProduct(String id) {
        ProductDAO dao = daoFactory.getDAO(DAOFactory.DAOType.PRODUCT);
        int r = dao.delete(id);
        audit("DELETE_PRODUCT", "Deleted product ID: " + id, "C-04");
        eventBus.publish(EventBus.Event.PRODUCT_DELETED, id);
        return r;
    }

    // ── Stock Levels (C-04) ──────────────────────────────────────────────────

    public List<StockLevel> getAllStockLevels() {
        return ((StockLevelDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_LEVEL)).findAll();
    }

    public List<StockLevel> getLowStockItems() {
        return ((StockLevelDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_LEVEL)).findLowStock();
    }

    public int createStockLevel(StockLevel s) {
        int r = ((StockLevelDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_LEVEL)).insert(s);
        audit("CREATE_STOCK", "Stock level created for product: " + s.getProductId(), "C-04");
        eventBus.publish(EventBus.Event.STOCK_CHANGED, s);
        return r;
    }

    public int updateStockLevel(StockLevel s) {
        int r = ((StockLevelDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_LEVEL)).update(s);
        audit("UPDATE_STOCK", "Stock updated for product: " + s.getProductId(), "C-04");
        eventBus.publish(EventBus.Event.STOCK_CHANGED, s);
        return r;
    }

    public int deleteStockLevel(String id) {
        return ((StockLevelDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_LEVEL)).delete(id);
    }

    // ── Stock Movements ──────────────────────────────────────────────────────

    public List<StockMovement> getAllStockMovements() {
        return ((StockMovementDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_MOVEMENT)).findAll();
    }

    public int createStockMovement(StockMovement m) {
        int r = ((StockMovementDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_MOVEMENT)).insert(m);
        audit("STOCK_MOVE", "Movement " + m.getMovementType() + " qty=" + m.getMovedQty(), "C-04");
        eventBus.publish(EventBus.Event.STOCK_CHANGED, m);
        return r;
    }

    public int deleteStockMovement(String id) {
        return ((StockMovementDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_MOVEMENT)).delete(id);
    }

    // ── Stock Records ────────────────────────────────────────────────────────

    public List<StockRecord> getAllStockRecords() {
        return ((StockRecordDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_RECORD)).findAll();
    }

    public int createStockRecord(StockRecord r) {
        return ((StockRecordDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_RECORD)).insert(r);
    }

    public int updateStockRecord(StockRecord r) {
        return ((StockRecordDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_RECORD)).update(r);
    }

    public int deleteStockRecord(String id) {
        return ((StockRecordDAO) daoFactory.getDAO(DAOFactory.DAOType.STOCK_RECORD)).delete(id);
    }

    public List<WarehouseProductStock> getWarehouseProductStocks() {
        return daoFactory.<StockRecordDAO>getDAO(DAOFactory.DAOType.STOCK_RECORD)
                .findWarehouseProductStocks();
    }

    // ── Warehouses & Bins ────────────────────────────────────────────────────

    public List<Warehouse> getAllWarehouses() {
        return ((WarehouseDAO) daoFactory.getDAO(DAOFactory.DAOType.WAREHOUSE)).findAll();
    }

    public int createWarehouse(Warehouse w) {
        int r = ((WarehouseDAO) daoFactory.getDAO(DAOFactory.DAOType.WAREHOUSE)).insert(w);
        audit("CREATE_WAREHOUSE", "Warehouse: " + w.getWarehouseName(), "C-04");
        return r;
    }

    public int updateWarehouse(Warehouse w) {
        return ((WarehouseDAO) daoFactory.getDAO(DAOFactory.DAOType.WAREHOUSE)).update(w);
    }

    public int deleteWarehouse(String id) {
        return ((WarehouseDAO) daoFactory.getDAO(DAOFactory.DAOType.WAREHOUSE)).delete(id);
    }

    public List<Bin> getAllBins() {
        return ((BinDAO) daoFactory.getDAO(DAOFactory.DAOType.BIN)).findAll();
    }

    public int createBin(Bin b) {
        return ((BinDAO) daoFactory.getDAO(DAOFactory.DAOType.BIN)).insert(b);
    }

    public int updateBin(Bin b) {
        return ((BinDAO) daoFactory.getDAO(DAOFactory.DAOType.BIN)).update(b);
    }

    public int deleteBin(String id) {
        return ((BinDAO) daoFactory.getDAO(DAOFactory.DAOType.BIN)).delete(id);
    }

    // ── Barcode Events ───────────────────────────────────────────────────────

    public List<BarcodeEvent> getAllBarcodeEvents() {
        return ((BarcodeEventDAO) daoFactory.getDAO(DAOFactory.DAOType.BARCODE_EVENT)).findAll();
    }

    public int createBarcodeEvent(BarcodeEvent e) {
        return ((BarcodeEventDAO) daoFactory.getDAO(DAOFactory.DAOType.BARCODE_EVENT)).insert(e);
    }

    public int deleteBarcodeEvent(String id) {
        return ((BarcodeEventDAO) daoFactory.getDAO(DAOFactory.DAOType.BARCODE_EVENT)).delete(id);
    }

    // ── Orders ───────────────────────────────────────────────────────────────

    public List<Order> getAllOrders() {
        return ((OrderDAO) daoFactory.getDAO(DAOFactory.DAOType.ORDER)).findAll();
    }

    public int createOrder(Order o) {
        int r = ((OrderDAO) daoFactory.getDAO(DAOFactory.DAOType.ORDER)).insert(o);
        audit("CREATE_ORDER", "Order created: " + o.getOrderId(), "C-05");
        eventBus.publish(EventBus.Event.ORDER_CREATED, o);
        return r;
    }

    public int updateOrder(Order o) {
        int r = ((OrderDAO) daoFactory.getDAO(DAOFactory.DAOType.ORDER)).update(o);
        audit("UPDATE_ORDER", "Order updated: " + o.getOrderId(), "C-05");
        eventBus.publish(EventBus.Event.ORDER_UPDATED, o);
        return r;
    }

    public int deleteOrder(String id) {
        return ((OrderDAO) daoFactory.getDAO(DAOFactory.DAOType.ORDER)).delete(id);
    }

    // ── Order Items ──────────────────────────────────────────────────────────

    public List<OrderItem> getAllOrderItems() {
        return ((OrderItemDAO) daoFactory.getDAO(DAOFactory.DAOType.ORDER_ITEM)).findAll();
    }

    public List<OrderItem> getOrderItems(String orderId) {
        return ((OrderItemDAO) daoFactory.getDAO(DAOFactory.DAOType.ORDER_ITEM)).findByOrder(orderId);
    }

    public int createOrderItem(OrderItem i) {
        return ((OrderItemDAO) daoFactory.getDAO(DAOFactory.DAOType.ORDER_ITEM)).insert(i);
    }

    public int updateOrderItem(OrderItem i) {
        return ((OrderItemDAO) daoFactory.getDAO(DAOFactory.DAOType.ORDER_ITEM)).update(i);
    }

    public int deleteOrderItem(String id) {
        return ((OrderItemDAO) daoFactory.getDAO(DAOFactory.DAOType.ORDER_ITEM)).delete(id);
    }

    // ── Fulfillment ──────────────────────────────────────────────────────────

    public List<FulfillmentOrder> getAllFulfillments() {
        return ((FulfillmentDAO) daoFactory.getDAO(DAOFactory.DAOType.FULFILLMENT)).findAll();
    }

    public int createFulfillment(FulfillmentOrder f) {
        int r = ((FulfillmentDAO) daoFactory.getDAO(DAOFactory.DAOType.FULFILLMENT)).insert(f);
        audit("CREATE_FULFILLMENT", "Fulfillment: " + f.getFulfillmentId(), "C-05");
        return r;
    }

    public int updateFulfillment(FulfillmentOrder f) {
        return ((FulfillmentDAO) daoFactory.getDAO(DAOFactory.DAOType.FULFILLMENT)).update(f);
    }

    public int deleteFulfillment(String id) {
        return ((FulfillmentDAO) daoFactory.getDAO(DAOFactory.DAOType.FULFILLMENT)).delete(id);
    }

    // ── Logistics ────────────────────────────────────────────────────────────

    public List<DeliveryOrder> getAllDeliveries() {
        return ((DeliveryDAO) daoFactory.getDAO(DAOFactory.DAOType.DELIVERY)).findAll();
    }

    public int createDelivery(DeliveryOrder d) {
        int r = ((DeliveryDAO) daoFactory.getDAO(DAOFactory.DAOType.DELIVERY)).insert(d);
        audit("CREATE_DELIVERY", "Delivery: " + d.getDeliveryId(), "C-06");
        eventBus.publish(EventBus.Event.DELIVERY_STATUS_CHANGED, d);
        return r;
    }

    /**
     * DB fallback for delivery-order creation when delivery JAR flow rejects input.
     * Persists pickup/dropoff and coordinates in orders (if columns exist), then
     * inserts delivery_orders row.
     */
    public String createDeliveryOrderWithGeo(String customerId,
                                             String pickupAddress,
                                             String dropoffAddress,
                                             double pickupLat,
                                             double pickupLng,
                                             double dropoffLat,
                                             double dropoffLng) {
        if (isBlank(customerId) || isBlank(pickupAddress) || isBlank(dropoffAddress)) return null;

        String orderId = "ORD-UI" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String deliveryId = "SHIP-UI" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean hasStatus = columnExists(conn, "orders", "status");
                boolean hasPickupAddress = columnExists(conn, "orders", "pickup_address");
                boolean hasDropoffAddress = columnExists(conn, "orders", "dropoff_address");
                boolean hasPickupLat = columnExists(conn, "orders", "pickup_latitude");
                boolean hasPickupLng = columnExists(conn, "orders", "pickup_longitude");
                boolean hasDropoffLat = columnExists(conn, "orders", "dropoff_latitude");
                boolean hasDropoffLng = columnExists(conn, "orders", "dropoff_longitude");
                boolean hasCreatedAt = columnExists(conn, "orders", "created_at");

                StringBuilder cols = new StringBuilder("order_id, customer_id, order_status, order_date, total_amount, payment_status, sales_channel");
                StringBuilder vals = new StringBuilder("?, ?, ?, NOW(), ?, ?, ?");

                if (hasStatus) { cols.append(", status"); vals.append(", ?"); }
                if (hasPickupAddress) { cols.append(", pickup_address"); vals.append(", ?"); }
                if (hasDropoffAddress) { cols.append(", dropoff_address"); vals.append(", ?"); }
                if (hasPickupLat) { cols.append(", pickup_latitude"); vals.append(", ?"); }
                if (hasPickupLng) { cols.append(", pickup_longitude"); vals.append(", ?"); }
                if (hasDropoffLat) { cols.append(", dropoff_latitude"); vals.append(", ?"); }
                if (hasDropoffLng) { cols.append(", dropoff_longitude"); vals.append(", ?"); }
                if (hasCreatedAt) { cols.append(", created_at"); vals.append(", NOW()"); }

                String insertOrder = "INSERT INTO orders(" + cols + ") VALUES(" + vals + ")";
                try (PreparedStatement ps = conn.prepareStatement(insertOrder)) {
                    int i = 1;
                    ps.setString(i++, orderId);
                    ps.setString(i++, customerId.trim());
                    ps.setString(i++, "PLACED");
                    ps.setDouble(i++, 0.0);
                    ps.setString(i++, "PENDING");
                    ps.setString(i++, "ONLINE");
                    if (hasStatus) ps.setString(i++, "CREATED");
                    if (hasPickupAddress) ps.setString(i++, pickupAddress.trim());
                    if (hasDropoffAddress) ps.setString(i++, dropoffAddress.trim());
                    if (hasPickupLat) ps.setDouble(i++, pickupLat);
                    if (hasPickupLng) ps.setDouble(i++, pickupLng);
                    if (hasDropoffLat) ps.setDouble(i++, dropoffLat);
                    if (hasDropoffLng) ps.setDouble(i++, dropoffLng);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO delivery_orders " +
                                "(delivery_id, order_id, customer_id, delivery_address, delivery_status, delivery_type, delivery_cost, assigned_agent, warehouse_id, created_at, updated_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())")) {
                    ps.setString(1, deliveryId);
                    ps.setString(2, orderId);
                    ps.setString(3, customerId.trim());
                    ps.setString(4, dropoffAddress.trim());
                    ps.setString(5, "CREATED");
                    ps.setString(6, "STANDARD");
                    ps.setDouble(7, 0.0);
                    ps.setString(8, null);
                    ps.setString(9, inferWarehouseIdFromPickup(pickupAddress));
                    ps.executeUpdate();
                }

                conn.commit();
                return orderId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Fallback delivery-order creation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Persists pickup/dropoff addresses and coordinates for an existing order created
     * by the delivery JAR so Live GPS grid can read consistent geo data from orders.
     */
    public boolean syncOrderGeoSnapshot(String orderId,
                                        String customerId,
                                        String pickupAddress,
                                        String dropoffAddress,
                                        double pickupLat,
                                        double pickupLng,
                                        double dropoffLat,
                                        double dropoffLng) {
        if (isBlank(orderId) || isBlank(customerId) || isBlank(pickupAddress) || isBlank(dropoffAddress)) {
            return false;
        }

        String oid = orderId.trim();
        String cid = customerId.trim();
        String pickup = pickupAddress.trim();
        String dropoff = dropoffAddress.trim();

        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            boolean hasStatus = columnExists(conn, "orders", "status");
            boolean hasPickupAddress = columnExists(conn, "orders", "pickup_address");
            boolean hasDropoffAddress = columnExists(conn, "orders", "dropoff_address");
            boolean hasPickupLat = columnExists(conn, "orders", "pickup_latitude");
            boolean hasPickupLng = columnExists(conn, "orders", "pickup_longitude");
            boolean hasDropoffLat = columnExists(conn, "orders", "dropoff_latitude");
            boolean hasDropoffLng = columnExists(conn, "orders", "dropoff_longitude");
            boolean hasCreatedAt = columnExists(conn, "orders", "created_at");

            StringBuilder setSql = new StringBuilder("customer_id = ?");
            List<Object> updateParams = new ArrayList<>();
            updateParams.add(cid);
            if (hasStatus) {
                setSql.append(", status = ?");
                updateParams.add("CREATED");
            }
            if (hasPickupAddress) {
                setSql.append(", pickup_address = ?");
                updateParams.add(pickup);
            }
            if (hasDropoffAddress) {
                setSql.append(", dropoff_address = ?");
                updateParams.add(dropoff);
            }
            if (hasPickupLat) {
                setSql.append(", pickup_latitude = ?");
                updateParams.add(pickupLat);
            }
            if (hasPickupLng) {
                setSql.append(", pickup_longitude = ?");
                updateParams.add(pickupLng);
            }
            if (hasDropoffLat) {
                setSql.append(", dropoff_latitude = ?");
                updateParams.add(dropoffLat);
            }
            if (hasDropoffLng) {
                setSql.append(", dropoff_longitude = ?");
                updateParams.add(dropoffLng);
            }
            if (hasCreatedAt) {
                setSql.append(", created_at = COALESCE(created_at, NOW())");
            }

            String updateSql = "UPDATE orders SET " + setSql + " WHERE order_id = ?";
            updateParams.add(oid);
            int updated;
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                int i = 1;
                for (Object param : updateParams) {
                    ps.setObject(i++, param);
                }
                updated = ps.executeUpdate();
            }
            if (updated > 0) {
                return true;
            }

            StringBuilder cols = new StringBuilder("order_id, customer_id, order_status, order_date, total_amount, payment_status, sales_channel");
            StringBuilder vals = new StringBuilder("?, ?, ?, NOW(), ?, ?, ?");
            if (hasStatus) { cols.append(", status"); vals.append(", ?"); }
            if (hasPickupAddress) { cols.append(", pickup_address"); vals.append(", ?"); }
            if (hasDropoffAddress) { cols.append(", dropoff_address"); vals.append(", ?"); }
            if (hasPickupLat) { cols.append(", pickup_latitude"); vals.append(", ?"); }
            if (hasPickupLng) { cols.append(", pickup_longitude"); vals.append(", ?"); }
            if (hasDropoffLat) { cols.append(", dropoff_latitude"); vals.append(", ?"); }
            if (hasDropoffLng) { cols.append(", dropoff_longitude"); vals.append(", ?"); }
            if (hasCreatedAt) { cols.append(", created_at"); vals.append(", NOW()"); }

            String insertSql = "INSERT INTO orders(" + cols + ") VALUES(" + vals + ")";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                int i = 1;
                ps.setString(i++, oid);
                ps.setString(i++, cid);
                ps.setString(i++, "PLACED");
                ps.setDouble(i++, 0.0);
                ps.setString(i++, "PENDING");
                ps.setString(i++, "ONLINE");
                if (hasStatus) ps.setString(i++, "CREATED");
                if (hasPickupAddress) ps.setString(i++, pickup);
                if (hasDropoffAddress) ps.setString(i++, dropoff);
                if (hasPickupLat) ps.setDouble(i++, pickupLat);
                if (hasPickupLng) ps.setDouble(i++, pickupLng);
                if (hasDropoffLat) ps.setDouble(i++, dropoffLat);
                if (hasDropoffLng) ps.setDouble(i++, dropoffLng);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to sync order geo snapshot: " + e.getMessage());
            return false;
        }
    }

    public int updateDelivery(DeliveryOrder d) {
        int r = ((DeliveryDAO) daoFactory.getDAO(DAOFactory.DAOType.DELIVERY)).update(d);
        eventBus.publish(EventBus.Event.DELIVERY_STATUS_CHANGED, d);
        return r;
    }

    public int deleteDelivery(String id) {
        return ((DeliveryDAO) daoFactory.getDAO(DAOFactory.DAOType.DELIVERY)).delete(id);
    }

    public List<Shipment> getAllShipments() {
        return ((ShipmentDAO) daoFactory.getDAO(DAOFactory.DAOType.SHIPMENT)).findAll();
    }

    public int createShipment(Shipment s) {
        int r = ((ShipmentDAO) daoFactory.getDAO(DAOFactory.DAOType.SHIPMENT)).insert(s);
        audit("CREATE_SHIPMENT", "Shipment: " + s.getShipmentId(), "C-06");
        return r;
    }

    public int updateShipment(Shipment s) {
        return ((ShipmentDAO) daoFactory.getDAO(DAOFactory.DAOType.SHIPMENT)).update(s);
    }

    public int deleteShipment(String id) {
        return ((ShipmentDAO) daoFactory.getDAO(DAOFactory.DAOType.SHIPMENT)).delete(id);
    }

    public List<ShipmentMapPoint> getShipmentLiveMapPoints() {
        return ((ShipmentDAO) daoFactory.getDAO(DAOFactory.DAOType.SHIPMENT)).findLiveMapPoints();
    }

    /**
     * Reads delivery status for an order directly from DB compatibility tables.
     */
    public String getDeliveryStatusForOrder(String orderId) {
        if (isBlank(orderId)) return null;
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            boolean hasOrderStatus = columnExists(conn, "orders", "status");
            String sql = hasOrderStatus
                    ? "SELECT COALESCE(o.status, d.delivery_status) AS status " +
                      "FROM orders o LEFT JOIN delivery_orders d ON d.order_id = o.order_id " +
                      "WHERE o.order_id = ? LIMIT 1"
                    : "SELECT d.delivery_status AS status FROM delivery_orders d WHERE d.order_id = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, orderId.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("status");
                }
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to read delivery status: " + e.getMessage());
        }
        return null;
    }

    /**
     * Reads tracking URL for an order directly from DB compatibility tables.
     */
    public String getDeliveryTrackingUrl(String orderId) {
        if (isBlank(orderId)) return null;
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            if (!tableExists(conn, "delivery_tracking_routes")) {
                return null;
            }
            String sql =
                    "SELECT dtr.tracking_api_url " +
                    "FROM delivery_orders d " +
                    "JOIN delivery_tracking_routes dtr ON dtr.delivery_id = d.delivery_id " +
                    "WHERE d.order_id = ? " +
                    "ORDER BY dtr.current_eta DESC " +
                    "LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, orderId.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("tracking_api_url");
                }
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to read tracking URL: " + e.getMessage());
        }
        return null;
    }

    /**
     * Assigns rider in DB tables for an existing order.
     * Returns number of affected rows.
     */
    public int assignDeliveryRiderForOrder(String orderId, String riderId) {
        if (isBlank(orderId) || isBlank(riderId)) return 0;
        int affected = 0;
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE delivery_orders SET assigned_agent = ?, updated_at = NOW() WHERE order_id = ?")) {
                ps.setString(1, riderId.trim());
                ps.setString(2, orderId.trim());
                affected += ps.executeUpdate();
            }
            if (columnExists(conn, "orders", "rider_id")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE orders SET rider_id = ? WHERE order_id = ?")) {
                    ps.setString(1, riderId.trim());
                    ps.setString(2, orderId.trim());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to assign rider in DB: " + e.getMessage());
        }
        return affected;
    }

    /**
     * Updates delivery status in DB tables for an existing order.
     * Returns number of affected rows.
     */
    public int updateDeliveryStatusForOrder(String orderId, String status, String changedBy) {
        if (isBlank(orderId) || isBlank(status)) return 0;
        int affected = 0;
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE delivery_orders SET delivery_status = ?, updated_at = NOW() WHERE order_id = ?")) {
                ps.setString(1, status.trim());
                ps.setString(2, orderId.trim());
                affected += ps.executeUpdate();
            }
            if (columnExists(conn, "orders", "status")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE orders SET status = ? WHERE order_id = ?")) {
                    ps.setString(1, status.trim());
                    ps.setString(2, orderId.trim());
                    ps.executeUpdate();
                }
            }
            if (tableExists(conn, "delivery_status_log")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO delivery_status_log (log_id, order_id, status, trigger_source, changed_by, changed_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())")) {
                    ps.setString(1, "DSL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    ps.setString(2, orderId.trim());
                    ps.setString(3, status.trim());
                    ps.setString(4, "UI");
                    ps.setString(5, isBlank(changedBy) ? "SYSTEM" : changedBy.trim());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to update delivery status in DB: " + e.getMessage());
        }
        return affected;
    }

    /**
     * Returns delivery-monitoring rows for the Live GPS grid.
     * Data comes from DB-mode compatibility tables (orders/customers/gps_pings/etc.).
     */
    public List<Object[]> getDeliveryLiveRows() {
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            boolean hasCustomersTable = tableExists(conn, "customers");
            boolean hasGpsTable = tableExists(conn, "gps_pings");
            boolean hasRoutesTable = tableExists(conn, "delivery_tracking_routes");
            boolean hasTrackingEventsTable = tableExists(conn, "delivery_tracking_events");
            boolean hasOrderRider = columnExists(conn, "orders", "rider_id");
            boolean hasOrderStatus = columnExists(conn, "orders", "status");
            boolean hasPickupAddress = columnExists(conn, "orders", "pickup_address");
            boolean hasDropoffAddress = columnExists(conn, "orders", "dropoff_address");
            boolean hasPickupLat = columnExists(conn, "orders", "pickup_latitude");
            boolean hasPickupLng = columnExists(conn, "orders", "pickup_longitude");
            boolean hasDropoffLat = columnExists(conn, "orders", "dropoff_latitude");
            boolean hasDropoffLng = columnExists(conn, "orders", "dropoff_longitude");
            boolean hasOrderCreatedAt = columnExists(conn, "orders", "created_at");
            boolean hasOrderDate = columnExists(conn, "orders", "order_date");

            String riderExpr = hasOrderRider
                    ? "COALESCE(o.rider_id, d.assigned_agent, 'Unassigned')"
                    : "COALESCE(d.assigned_agent, 'Unassigned')";
            String statusExpr = hasOrderStatus
                    ? "COALESCE(o.status, d.delivery_status, 'CREATED')"
                    : "COALESCE(d.delivery_status, 'CREATED')";
            String pickupExpr = hasPickupAddress
                    ? "COALESCE(NULLIF(o.pickup_address, ''), " +
                      "CASE WHEN d.warehouse_id IS NULL THEN NULL ELSE CONCAT('Warehouse ', d.warehouse_id) END, '—')"
                    : "CASE WHEN d.warehouse_id IS NULL THEN '—' ELSE CONCAT('Warehouse ', d.warehouse_id) END";
            String dropoffExpr = hasDropoffAddress
                    ? "COALESCE(NULLIF(o.dropoff_address, ''), d.delivery_address, '—')"
                    : "COALESCE(d.delivery_address, '—')";
            String customerExpr = hasCustomersTable
                    ? "COALESCE(c.name, o.customer_id)"
                    : "o.customer_id";
            String sortExpr = hasOrderCreatedAt
                    ? "o.created_at"
                    : (hasOrderDate ? "o.order_date" : "d.created_at");

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT o.order_id, ")
               .append(customerExpr).append(" AS customer_name, ")
               .append(riderExpr).append(" AS rider_id, ")
               .append(statusExpr).append(" AS delivery_status, ");

            if (hasGpsTable || hasTrackingEventsTable || hasPickupLat || hasPickupLng || hasDropoffLat || hasDropoffLng) {
                String latExpr = hasPickupLat && hasDropoffLat
                        ? "COALESCE(o.pickup_latitude, o.dropoff_latitude)"
                        : (hasPickupLat ? "o.pickup_latitude" : (hasDropoffLat ? "o.dropoff_latitude" : "NULL"));
                String lngExpr = hasPickupLng && hasDropoffLng
                        ? "COALESCE(o.pickup_longitude, o.dropoff_longitude)"
                        : (hasPickupLng ? "o.pickup_longitude" : (hasDropoffLng ? "o.dropoff_longitude" : "NULL"));

                if (hasGpsTable) {
                    latExpr = "COALESCE(gp.latitude, " + latExpr + ")";
                    lngExpr = "COALESCE(gp.longitude, " + lngExpr + ")";
                }

                if (hasTrackingEventsTable) {
                    latExpr = "COALESCE(" + latExpr + ", " +
                            "CASE WHEN dte.gps_coordinates LIKE '%,%' " +
                            "THEN CAST(TRIM(SUBSTRING_INDEX(dte.gps_coordinates, ',', 1)) AS DECIMAL(10,6)) " +
                            "ELSE NULL END)";
                    lngExpr = "COALESCE(" + lngExpr + ", " +
                            "CASE WHEN dte.gps_coordinates LIKE '%,%' " +
                            "THEN CAST(TRIM(SUBSTRING_INDEX(dte.gps_coordinates, ',', -1)) AS DECIMAL(10,6)) " +
                            "ELSE NULL END)";
                }

                sql.append(latExpr).append(" AS latitude, ")
                   .append(lngExpr).append(" AS longitude, ");
            } else {
                sql.append("NULL AS latitude, NULL AS longitude, ");
            }

            if (hasRoutesTable) {
                sql.append("CASE WHEN dtr.current_eta IS NULL THEN NULL ")
                   .append("ELSE GREATEST(TIMESTAMPDIFF(MINUTE, NOW(), dtr.current_eta), 0) END AS eta_minutes, ");
            } else {
                sql.append("NULL AS eta_minutes, ");
            }

            sql.append(pickupExpr).append(" AS pickup_address, ")
               .append(dropoffExpr).append(" AS dropoff_address ")
               .append("FROM orders o ")
               .append("LEFT JOIN delivery_orders d ON d.order_id = o.order_id ");

            if (hasCustomersTable) {
                sql.append("LEFT JOIN customers c ON c.customer_id = o.customer_id ");
            }

            if (hasGpsTable) {
                sql.append("LEFT JOIN gps_pings gp ON gp.rider_id = ")
                   .append(riderExpr)
                   .append(" AND gp.ping_timestamp = (")
                   .append("SELECT MAX(g2.ping_timestamp) FROM gps_pings g2 WHERE g2.rider_id = ")
                   .append(riderExpr)
                   .append(") ");
            }

            if (hasRoutesTable) {
                sql.append("LEFT JOIN delivery_tracking_routes dtr ON dtr.delivery_id = d.delivery_id ");
            }

            if (hasTrackingEventsTable) {
                sql.append("LEFT JOIN delivery_tracking_events dte ON dte.delivery_id = d.delivery_id ")
                   .append("AND dte.event_timestamp = (")
                   .append("SELECT MAX(dte2.event_timestamp) FROM delivery_tracking_events dte2 WHERE dte2.delivery_id = d.delivery_id")
                   .append(") ");
            }

            sql.append("WHERE d.delivery_id IS NOT NULL ")
               .append("ORDER BY ").append(sortExpr).append(" DESC ")
               .append("LIMIT 200");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString());
                 ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object latObj = rs.getObject("latitude");
                Object lngObj = rs.getObject("longitude");
                Object etaObj = rs.getObject("eta_minutes");
                rows.add(new Object[]{
                        rs.getString("order_id"),
                        rs.getString("customer_name"),
                        rs.getString("rider_id"),
                        rs.getString("delivery_status"),
                        latObj == null ? "—" : String.format("%.5f", rs.getDouble("latitude")),
                        lngObj == null ? "—" : String.format("%.5f", rs.getDouble("longitude")),
                        etaObj == null ? "—" : String.valueOf(rs.getInt("eta_minutes")),
                        rs.getString("pickup_address"),
                        rs.getString("dropoff_address")
                });
            }
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to load delivery live rows: " + e.getMessage());
        }
        return rows;
    }

    public double[] getLatestRiderCoordinates(String riderId) {
        if (isBlank(riderId)) return null;
        String rid = riderId.trim();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            if (tableExists(conn, "gps_pings")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT latitude, longitude FROM gps_pings WHERE rider_id = ? ORDER BY ping_timestamp DESC LIMIT 1")) {
                    ps.setString(1, rid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return new double[]{rs.getDouble("latitude"), rs.getDouble("longitude")};
                    }
                }
            }

            if (tableExists(conn, "delivery_orders") && tableExists(conn, "delivery_tracking_events")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT " +
                                "CAST(TRIM(SUBSTRING_INDEX(dte.gps_coordinates, ',', 1)) AS DECIMAL(10,6)) AS latitude, " +
                                "CAST(TRIM(SUBSTRING_INDEX(dte.gps_coordinates, ',', -1)) AS DECIMAL(10,6)) AS longitude " +
                                "FROM delivery_orders d " +
                                "JOIN delivery_tracking_events dte ON dte.delivery_id = d.delivery_id " +
                                "WHERE d.assigned_agent = ? AND dte.gps_coordinates LIKE '%,%' " +
                                "ORDER BY dte.event_timestamp DESC LIMIT 1")) {
                    ps.setString(1, rid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return new double[]{rs.getDouble("latitude"), rs.getDouble("longitude")};
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to read rider coordinates: " + e.getMessage());
        }
        return null;
    }

    /**
     * DB-first fallback for POD submission when delivery facade flow is unavailable.
     */
    public boolean completeDeliveryWithPodFallback(String orderId, String signatureUrl, String photoUrl, String notes) {
        if (isBlank(orderId)) return false;
        String oid = orderId.trim();

        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            if (!tableExists(conn, "pod_records")) return false;

            String riderId = resolveRiderIdForOrder(conn, oid);
            if (isBlank(riderId)) riderId = "UNASSIGNED";

            String existingPodId = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT pod_id FROM pod_records WHERE order_id = ? ORDER BY submitted_at DESC LIMIT 1")) {
                ps.setString(1, oid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) existingPodId = rs.getString("pod_id");
                }
            }

            if (existingPodId != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE pod_records SET rider_id = ?, signature_url = ?, photo_url = ?, notes = ?, submitted_at = NOW() " +
                        "WHERE pod_id = ?")) {
                    ps.setString(1, riderId);
                    ps.setString(2, normalizeOptionalText(signatureUrl));
                    ps.setString(3, normalizeOptionalText(photoUrl));
                    ps.setString(4, normalizeOptionalText(notes));
                    ps.setString(5, existingPodId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO pod_records (pod_id, order_id, rider_id, signature_url, photo_url, notes, submitted_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setString(1, "POD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    ps.setString(2, oid);
                    ps.setString(3, riderId);
                    ps.setString(4, normalizeOptionalText(signatureUrl));
                    ps.setString(5, normalizeOptionalText(photoUrl));
                    ps.setString(6, normalizeOptionalText(notes));
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to upsert POD fallback: " + e.getMessage());
            return false;
        }

        updateDeliveryStatusForOrder(oid, "DELIVERED", "SYSTEM");
        return true;
    }

    /**
     * Returns latest POD details row for an order:
     * [order_id, rider_id, signature_url, photo_url, notes, submitted_at].
     */
    public Object[] getPodDetailsForOrder(String orderId) {
        if (isBlank(orderId)) return null;
        String oid = orderId.trim();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            if (!tableExists(conn, "pod_records")) return null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT order_id, rider_id, signature_url, photo_url, notes, submitted_at " +
                    "FROM pod_records WHERE order_id = ? ORDER BY submitted_at DESC LIMIT 1")) {
                ps.setString(1, oid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    return new Object[]{
                            rs.getString("order_id"),
                            rs.getString("rider_id"),
                            rs.getString("signature_url"),
                            rs.getString("photo_url"),
                            rs.getString("notes"),
                            rs.getTimestamp("submitted_at")
                    };
                }
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to read POD details fallback: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reads ETA minutes from DB compatibility tables for an order.
     */
    public Integer getLatestEtaMinutesForOrder(String orderId) {
        if (isBlank(orderId)) return null;
        String oid = orderId.trim();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            if (tableExists(conn, "delivery_tracking_routes") && tableExists(conn, "delivery_orders")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT CASE WHEN dtr.current_eta IS NULL THEN NULL " +
                        "ELSE GREATEST(TIMESTAMPDIFF(MINUTE, NOW(), dtr.current_eta), 0) END AS eta_minutes " +
                        "FROM delivery_orders d " +
                        "LEFT JOIN delivery_tracking_routes dtr ON dtr.delivery_id = d.delivery_id " +
                        "WHERE d.order_id = ? " +
                        "ORDER BY dtr.current_eta DESC LIMIT 1")) {
                    ps.setString(1, oid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Object etaObj = rs.getObject("eta_minutes");
                            if (etaObj != null) return rs.getInt("eta_minutes");
                        }
                    }
                }
            }

            String status = getDeliveryStatusForOrder(oid);
            if ("DELIVERED".equalsIgnoreCase(status)) return 0;
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to read ETA fallback: " + e.getMessage());
        }
        return null;
    }

    /**
     * Persists a simulated GPS ping in DB tables and marks order IN_TRANSIT.
     */
    public boolean saveGpsPingAndMarkTransit(String deviceId, String orderId, double lat, double lng) {
        if (isBlank(orderId) || isBlank(deviceId)) return false;
        String oid = orderId.trim();
        String did = deviceId.trim();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            if (!tableExists(conn, "gps_pings")) return false;

            String riderId = resolveRiderIdForOrder(conn, oid);
            if (isBlank(riderId) && tableExists(conn, "devices")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT rider_id FROM devices WHERE device_id = ? LIMIT 1")) {
                    ps.setString(1, did);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) riderId = rs.getString("rider_id");
                    }
                }
            }
            if (isBlank(riderId)) return false;

            if (tableExists(conn, "riders")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO riders (rider_id, name, phone, vehicle_type, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE rider_id = VALUES(rider_id)")) {
                    ps.setString(1, riderId);
                    ps.setString(2, "Auto-Linked Rider");
                    ps.setString(3, "");
                    ps.setString(4, "BIKE");
                    ps.setString(5, "ON_DELIVERY");
                    ps.executeUpdate();
                }
            }

            if (tableExists(conn, "devices")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO devices (device_id, rider_id, online, created_at) " +
                        "VALUES (?, ?, TRUE, NOW()) " +
                        "ON DUPLICATE KEY UPDATE rider_id = VALUES(rider_id), online = TRUE")) {
                    ps.setString(1, did);
                    ps.setString(2, riderId);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO gps_pings (ping_id, device_id, rider_id, latitude, longitude, ping_timestamp, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, NOW(), NOW())")) {
                ps.setString(1, "PING-UI" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                ps.setString(2, did);
                ps.setString(3, riderId);
                ps.setDouble(4, lat);
                ps.setDouble(5, lng);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to persist GPS fallback ping: " + e.getMessage());
            return false;
        }

        updateDeliveryStatusForOrder(oid, "IN_TRANSIT", "SYSTEM");
        return true;
    }

    /**
     * Returns status history lines from DB for an order.
     */
    public List<String> getDeliveryStatusHistoryLines(String orderId) {
        List<String> lines = new ArrayList<>();
        if (isBlank(orderId)) return lines;
        String oid = orderId.trim();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            if (tableExists(conn, "delivery_status_log")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT status, trigger_source, changed_by, changed_at " +
                        "FROM delivery_status_log WHERE order_id = ? ORDER BY changed_at ASC")) {
                    ps.setString(1, oid);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            lines.add(String.format("%s | %-12s | by=%s | src=%s",
                                    rs.getTimestamp("changed_at"),
                                    rs.getString("status"),
                                    rs.getString("changed_by"),
                                    rs.getString("trigger_source")));
                        }
                    }
                }
            }
            if (lines.isEmpty()) {
                String status = getDeliveryStatusForOrder(oid);
                if (!isBlank(status)) {
                    lines.add("Current Status: " + status);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to read status history fallback: " + e.getMessage());
        }
        return lines;
    }

    /**
     * Returns POD rows for the POD tab table.
     */
    public List<Object[]> getDeliveryPodRows() {
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection()) {
            if (!tableExists(conn, "pod_records")) {
                return rows;
            }

            String sql =
                    "SELECT p.order_id, p.rider_id, p.signature_url, p.photo_url, p.notes, p.submitted_at " +
                    "FROM pod_records p " +
                    "ORDER BY p.submitted_at DESC " +
                    "LIMIT 200";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getString("order_id"),
                            rs.getString("rider_id"),
                            rs.getString("signature_url"),
                            rs.getString("photo_url"),
                            rs.getString("notes"),
                            rs.getTimestamp("submitted_at") == null ? "—" : rs.getTimestamp("submitted_at").toString()
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("[SCM] Failed to load POD rows: " + e.getMessage());
        }
        return rows;
    }

    private String resolveRiderIdForOrder(Connection conn, String orderId) throws SQLException {
        if (columnExists(conn, "orders", "rider_id")) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT rider_id FROM orders WHERE order_id = ? LIMIT 1")) {
                ps.setString(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && !isBlank(rs.getString("rider_id"))) {
                        return rs.getString("rider_id");
                    }
                }
            }
        }
        if (tableExists(conn, "delivery_orders")) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT assigned_agent FROM delivery_orders WHERE order_id = ? LIMIT 1")) {
                ps.setString(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && !isBlank(rs.getString("assigned_agent"))) {
                        return rs.getString("assigned_agent");
                    }
                }
            }
        }
        return null;
    }

    private String normalizeOptionalText(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean tableExists(Connection conn, String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) {
        String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    private String inferWarehouseIdFromPickup(String pickupAddress) {
        if (pickupAddress == null) return "WH-001";
        String token = pickupAddress.split(",")[0].trim();
        return token.matches("WH-\\d+") ? token : "WH-001";
    }

    // ── Pricing ──────────────────────────────────────────────────────────────

    public List<PriceListEntry> getAllPrices() {
        return ((PriceListDAO) daoFactory.getDAO(DAOFactory.DAOType.PRICE_LIST)).findAll();
    }

    public int createPrice(PriceListEntry p) {
        int r = ((PriceListDAO) daoFactory.getDAO(DAOFactory.DAOType.PRICE_LIST)).insert(p);
        eventBus.publish(EventBus.Event.PRICE_CHANGED, p);
        return r;
    }

    public int updatePrice(PriceListEntry p) {
        int r = ((PriceListDAO) daoFactory.getDAO(DAOFactory.DAOType.PRICE_LIST)).update(p);
        eventBus.publish(EventBus.Event.PRICE_CHANGED, p);
        return r;
    }

    public int deletePrice(String id) {
        return ((PriceListDAO) daoFactory.getDAO(DAOFactory.DAOType.PRICE_LIST)).delete(id);
    }

    public List<Promotion> getAllPromotions() {
        return ((PromotionDAO) daoFactory.getDAO(DAOFactory.DAOType.PROMOTION)).findAll();
    }

    public int createPromotion(Promotion p) {
        int r = ((PromotionDAO) daoFactory.getDAO(DAOFactory.DAOType.PROMOTION)).insert(p);
        eventBus.publish(EventBus.Event.PROMOTION_UPDATED, p);
        return r;
    }

    public int updatePromotion(Promotion p) {
        int r = ((PromotionDAO) daoFactory.getDAO(DAOFactory.DAOType.PROMOTION)).update(p);
        eventBus.publish(EventBus.Event.PROMOTION_UPDATED, p);
        return r;
    }

    public int deletePromotion(String id) {
        return ((PromotionDAO) daoFactory.getDAO(DAOFactory.DAOType.PROMOTION)).delete(id);
    }

    public List<DiscountPolicy> getAllDiscountPolicies() {
        return ((DiscountPolicyDAO) daoFactory.getDAO(DAOFactory.DAOType.DISCOUNT_POLICY)).findAll();
    }

    public int createDiscountPolicy(DiscountPolicy d) {
        return ((DiscountPolicyDAO) daoFactory.getDAO(DAOFactory.DAOType.DISCOUNT_POLICY)).insert(d);
    }

    public int updateDiscountPolicy(DiscountPolicy d) {
        return ((DiscountPolicyDAO) daoFactory.getDAO(DAOFactory.DAOType.DISCOUNT_POLICY)).update(d);
    }

    public int deleteDiscountPolicy(String id) {
        return ((DiscountPolicyDAO) daoFactory.getDAO(DAOFactory.DAOType.DISCOUNT_POLICY)).delete(id);
    }

    public List<ContractPricing> getAllContracts() {
        return ((ContractPricingDAO) daoFactory.getDAO(DAOFactory.DAOType.CONTRACT_PRICING)).findAll();
    }

    public int createContract(ContractPricing c) {
        return ((ContractPricingDAO) daoFactory.getDAO(DAOFactory.DAOType.CONTRACT_PRICING)).insert(c);
    }

    public int updateContract(ContractPricing c) {
        return ((ContractPricingDAO) daoFactory.getDAO(DAOFactory.DAOType.CONTRACT_PRICING)).update(c);
    }

    public int deleteContract(String id) {
        return ((ContractPricingDAO) daoFactory.getDAO(DAOFactory.DAOType.CONTRACT_PRICING)).delete(id);
    }

    public List<PriceApproval> getAllPriceApprovals() {
        return ((PriceApprovalDAO) daoFactory.getDAO(DAOFactory.DAOType.PRICE_APPROVAL)).findAll();
    }

    public int createPriceApproval(PriceApproval a) {
        return ((PriceApprovalDAO) daoFactory.getDAO(DAOFactory.DAOType.PRICE_APPROVAL)).insert(a);
    }

    public int updateApprovalStatus(String approvalId, String status, String managerId) {
        return ((PriceApprovalDAO) daoFactory.getDAO(DAOFactory.DAOType.PRICE_APPROVAL))
                .updateStatus(approvalId, status, managerId);
    }

    public int deletePriceApproval(String id) {
        return ((PriceApprovalDAO) daoFactory.getDAO(DAOFactory.DAOType.PRICE_APPROVAL)).delete(id);
    }

    public List<TierDefinition> getAllTiers() {
        return ((TierDefinitionDAO) daoFactory.getDAO(DAOFactory.DAOType.TIER_DEFINITION)).findAll();
    }

    public int createTier(TierDefinition t) {
        return ((TierDefinitionDAO) daoFactory.getDAO(DAOFactory.DAOType.TIER_DEFINITION)).insert(t);
    }

    public int updateTier(TierDefinition t) {
        return ((TierDefinitionDAO) daoFactory.getDAO(DAOFactory.DAOType.TIER_DEFINITION)).update(t);
    }

    public int deleteTier(int id) {
        return ((TierDefinitionDAO) daoFactory.getDAO(DAOFactory.DAOType.TIER_DEFINITION)).delete(id);
    }

    public List<CustomerSegmentation> getAllCustomerSegments() {
        return ((CustomerSegmentDAO) daoFactory.getDAO(DAOFactory.DAOType.CUSTOMER_SEGMENT)).findAll();
    }

    public int createCustomerSegment(CustomerSegmentation c) {
        return ((CustomerSegmentDAO) daoFactory.getDAO(DAOFactory.DAOType.CUSTOMER_SEGMENT)).insert(c);
    }

    public int updateCustomerSegment(CustomerSegmentation c) {
        return ((CustomerSegmentDAO) daoFactory.getDAO(DAOFactory.DAOType.CUSTOMER_SEGMENT)).update(c);
    }

    public int deleteCustomerSegment(String id) {
        return ((CustomerSegmentDAO) daoFactory.getDAO(DAOFactory.DAOType.CUSTOMER_SEGMENT)).delete(id);
    }

    public List<CommissionEntry> getAllCommissions() {
        return ((CommissionDAO) daoFactory.getDAO(DAOFactory.DAOType.COMMISSION)).findAll();
    }

    public int createCommission(CommissionEntry c) {
        return ((CommissionDAO) daoFactory.getDAO(DAOFactory.DAOType.COMMISSION)).insert(c);
    }

    public int updateCommission(CommissionEntry c) {
        return ((CommissionDAO) daoFactory.getDAO(DAOFactory.DAOType.COMMISSION)).update(c);
    }

    public int deleteCommission(String id) {
        return ((CommissionDAO) daoFactory.getDAO(DAOFactory.DAOType.COMMISSION)).delete(id);
    }

    // ── Users ────────────────────────────────────────────────────────────────

    public List<UIUser> getAllUsers() {
        return ((UserDAO) daoFactory.getDAO(DAOFactory.DAOType.USER)).findAll();
    }

    public long createUser(UIUser u) {
        long r = ((UserDAO) daoFactory.getDAO(DAOFactory.DAOType.USER)).insert(u);
        audit("CREATE_USER", "User created: " + u.getUsername(), "C-10");
        return r;
    }

    public int updateUser(UIUser u) {
        int r = ((UserDAO) daoFactory.getDAO(DAOFactory.DAOType.USER)).update(u);
        audit("UPDATE_USER", "User updated: " + u.getUsername(), "C-10");
        return r;
    }

    public int deleteUser(int id) {
        return ((UserDAO) daoFactory.getDAO(DAOFactory.DAOType.USER)).delete(id);
    }

    public int persistPanelState(String panelId) {
        if (currentUser == null || isBlank(panelId)) return 0;
        int r = ((PanelStateDAO) daoFactory.getDAO(DAOFactory.DAOType.PANEL_STATE))
                .upsert(currentUser.getUserId(), panelId, panelId);
        if (r > 0) eventBus.publish(EventBus.Event.PANEL_CHANGED, panelId);
        return r;
    }

    // ── Audit Logs ───────────────────────────────────────────────────────────

    public List<AuditLog> getAllAuditLogs() {
        return ((AuditLogDAO) daoFactory.getDAO(DAOFactory.DAOType.AUDIT_LOG)).findAll(200);
    }

    public List<AuditLog> getAuditLogs(int limit) {
        return ((AuditLogDAO) daoFactory.getDAO(DAOFactory.DAOType.AUDIT_LOG))
                .findAll(limit <= 0 ? 100 : limit);
    }

    // ── Notifications ────────────────────────────────────────────────────────

    public List<UINotification> getAllNotifications() {
        if (currentUser == null) return Collections.emptyList();
        return ((NotificationDAO) daoFactory.getDAO(DAOFactory.DAOType.NOTIFICATION))
                .findAllByUser(currentUser.getUserId());
    }

    public long addNotification(String type, String message) {
        if (isBlank(message)) return 0L;
        UINotification n = new UINotification();
        n.setUserId(currentUser != null ? currentUser.getUserId() : 1);
        n.setNotificationType(isBlank(type) ? "SYSTEM" : type);
        n.setNotificationMessage(message.trim());
        long id = ((NotificationDAO) daoFactory.getDAO(DAOFactory.DAOType.NOTIFICATION)).insert(n);
        if (id > 0) {
            eventBus.publish(EventBus.Event.NOTIFICATION_RECEIVED, n);
        }
        return id;
    }

    public int markNotificationRead(int id) {
        return ((NotificationDAO) daoFactory.getDAO(DAOFactory.DAOType.NOTIFICATION)).markRead(id);
    }

    public int markAllNotificationsRead() {
        if (currentUser == null) return 0;
        return ((NotificationDAO) daoFactory.getDAO(DAOFactory.DAOType.NOTIFICATION))
                .markAllRead(currentUser.getUserId());
    }

    public int getUnreadNotificationCount() {
        if (currentUser == null) return 0;
        return ((NotificationDAO) daoFactory.getDAO(DAOFactory.DAOType.NOTIFICATION))
                .countUnread(currentUser.getUserId());
    }

    // ── Demand Forecast ──────────────────────────────────────────────────────

    public List<DemandForecast> getAllForecasts() {
        return ((ForecastDAO) daoFactory.getDAO(DAOFactory.DAOType.FORECAST)).findAll();
    }

    public int createForecast(DemandForecast df) {
        int r = ((ForecastDAO) daoFactory.getDAO(DAOFactory.DAOType.FORECAST)).insert(df);
        if (r > 0) eventBus.publish(EventBus.Event.FORECAST_GENERATED, df);
        return r;
    }

    public int updateForecast(DemandForecast df) {
        return ((ForecastDAO) daoFactory.getDAO(DAOFactory.DAOType.FORECAST)).update(df);
    }

    public int deleteForecast(String id) {
        return ((ForecastDAO) daoFactory.getDAO(DAOFactory.DAOType.FORECAST)).delete(id);
    }

    // ── Exceptions ───────────────────────────────────────────────────────────

    public List<SubsystemException> getAllExceptions() {
        return ((ExceptionDAO) daoFactory.getDAO(DAOFactory.DAOType.EXCEPTION)).findAll();
    }

    public int createException(SubsystemException e) {
        int r = ((ExceptionDAO) daoFactory.getDAO(DAOFactory.DAOType.EXCEPTION)).insert(e);
        if (r > 0) eventBus.publish(EventBus.Event.EXCEPTION_RAISED, e);
        return r;
    }

    public int resolveException(String id) {
        int r = ((ExceptionDAO) daoFactory.getDAO(DAOFactory.DAOType.EXCEPTION)).resolve(id);
        if (r > 0) eventBus.publish(EventBus.Event.EXCEPTION_RESOLVED, id);
        return r;
    }

    public int deleteException(String id) {
        return ((ExceptionDAO) daoFactory.getDAO(DAOFactory.DAOType.EXCEPTION)).delete(id);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INVENTORY SUBSYSTEM — PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns the quantity held in the in-memory inventory repository
     * for a specific sku / locationId / supplierId triple.
     * (Updated — supplierId now part of the key.)
     */
    private int getRepoQuantity(String sku, String locationId, String supplierId) {
        String effectiveSupplierId = isBlank(supplierId) ? "DEFAULT" : supplierId;
        if (inventoryService instanceof InventoryService concrete) {
            return concrete.peekStock(sku, locationId, effectiveSupplierId);
        }
        return inventoryService.getStock(sku, locationId, effectiveSupplierId);
    }

    /**
     * Ensures the in-memory inventory is seeded from the database on first access.
     *
     * UPDATED: InventoryItem now requires supplierId.  When hydrating from
     * stock_records, the supplierId is resolved from the supplier table linked
     * to each product, falling back to "DEFAULT" when no supplier is mapped.
     */
    private void ensureInventoryRepositoryHydrated() {
        if (inventoryRepositoryHydrated) return;
        synchronized (this) {
            if (inventoryRepositoryHydrated) return;

            // Build product-id → SKU map
            ProductDAO productDAO = daoFactory.getDAO(DAOFactory.DAOType.PRODUCT);
            Map<String, String> productIdToSku      = new HashMap<>();
            Map<String, String> productIdToSupplier = new HashMap<>();
            for (Product p : productDAO.findAll()) {
                if (!isBlank(p.getProductId()) && !isBlank(p.getSku())) {
                    productIdToSku.put(
                            p.getProductId().trim().toUpperCase(Locale.ROOT),
                            p.getSku().trim()
                    );
                }
                // Resolve supplier linked to this product (may be null)
                if (!isBlank(p.getProductId()) && !isBlank(p.getSupplierId())) {
                    productIdToSupplier.put(
                            p.getProductId().trim().toUpperCase(Locale.ROOT),
                            p.getSupplierId().trim()
                    );
                }
            }

            // Seed from stock_records; each record is linked to a bin
            StockRecordDAO stockRecordDAO = daoFactory.getDAO(DAOFactory.DAOType.STOCK_RECORD);
            for (StockRecord record : stockRecordDAO.findAll()) {
                if (record == null
                        || isBlank(record.getBinId())
                        || isBlank(record.getProductId())) continue;
                int qty = Math.max(0, record.getQuantity());
                if (qty == 0) continue;

                String productId  = record.getProductId().trim();
                String sku        = productIdToSku.getOrDefault(
                                        productId.toUpperCase(Locale.ROOT), productId);
                String supplierId = productIdToSupplier.getOrDefault(
                                        productId.toUpperCase(Locale.ROOT), "DEFAULT");

                // Seed with supplierId — matches updated InventoryItem constructor
                inventoryService.addStock(sku, record.getBinId().trim(), supplierId, qty);

                // Also index by productId when it differs from SKU
                if (!sku.equalsIgnoreCase(productId)) {
                    inventoryService.addStock(productId, record.getBinId().trim(), supplierId, qty);
                }
            }

            inventoryRepositoryHydrated = true;
        }
    }

    /**
     * Quantity at a specific location, checking both canonical SKU and productId.
     * Updated to carry supplierId.
     */
    private int getQuantityAtLocation(Product product, String skuInput,
                                      String locationId, String supplierId) {
        if (isBlank(locationId)) return 0;
        String effectiveSupplierId = isBlank(supplierId) ? "DEFAULT" : supplierId;
        String canonicalSku = canonicalInventorySku(skuInput, product);

        int qty = inventoryService.getStock(canonicalSku, locationId, effectiveSupplierId);
        if (qty > 0) return qty;

        if (product != null && !isBlank(product.getProductId())
                && !product.getProductId().trim().equalsIgnoreCase(canonicalSku)) {
            qty = inventoryService.getStock(
                    product.getProductId().trim(), locationId, effectiveSupplierId);
        }
        return qty;
    }

    private void ensureStockLevelExists(String productId) {
        StockLevelDAO dao = daoFactory.getDAO(DAOFactory.DAOType.STOCK_LEVEL);
        StockLevel level = dao.findByProduct(productId);
        if (level != null) return;
        StockLevel created = new StockLevel();
        created.setStockLevelId(newStockLevelId());
        created.setProductId(productId);
        created.setCurrentStockQty(0);
        created.setReservedStockQty(0);
        created.setAvailableStockQty(0);
        created.setReorderThreshold(10);
        created.setReorderQuantity(50);
        created.setSafetyStockLevel(5);
        dao.insert(created);
    }

    private void applyStockLevelDelta(String productId, int delta) {
        if (isBlank(productId) || delta == 0) return;
        StockLevelDAO dao = daoFactory.getDAO(DAOFactory.DAOType.STOCK_LEVEL);
        StockLevel level = dao.findByProduct(productId);
        if (level == null) {
            ensureStockLevelExists(productId);
            level = dao.findByProduct(productId);
            if (level == null) return;
        }
        int updated = Math.max(0, level.getCurrentStockQty() + delta);
        level.setCurrentStockQty(updated);
        level.setAvailableStockQty(Math.max(0, updated - level.getReservedStockQty()));
        dao.update(level);
    }

    private void applyStockRecordDelta(String productId, String binId, int delta) {
        if (isBlank(productId) || isBlank(binId) || delta == 0) return;
        StockRecordDAO dao = daoFactory.getDAO(DAOFactory.DAOType.STOCK_RECORD);
        StockRecord existing = dao.findByProductAndBin(productId, binId);
        if (existing == null) {
            if (delta <= 0) return;
            StockRecord created = new StockRecord();
            created.setStockId(newStockRecordId());
            created.setProductId(productId);
            created.setBinId(binId);
            created.setQuantity(delta);
            dao.insert(created);
            return;
        }
        int updated = Math.max(0, existing.getQuantity() + delta);
        existing.setQuantity(updated);
        dao.update(existing);
    }

    private void insertStockMovement(String type, String fromBin, String toBin,
                                     String productId, int qty) {
        if (isBlank(type) || isBlank(productId) || qty <= 0) return;
        if (isBlank(fromBin) && isBlank(toBin)) return;
        if (!isBlank(fromBin) && !binExists(fromBin)) fromBin = null;
        if (!isBlank(toBin)   && !binExists(toBin))   toBin   = null;
        if (isBlank(fromBin) && isBlank(toBin)) return;

        StockMovement movement = new StockMovement();
        movement.setMovementId(newStockMovementId());
        movement.setMovementType(type);
        movement.setFromBin(fromBin);
        movement.setToBin(toBin);
        movement.setProductId(productId);
        movement.setMovedQty(qty);
        createStockMovement(movement);
    }

    private boolean binExists(String binId) {
        if (isBlank(binId)) return false;
        for (Bin b : getAllBins()) {
            if (binId.equalsIgnoreCase(b.getBinId())) return true;
        }
        return false;
    }

    private String resolveBinForLocation(String locationId) {
        if (isBlank(locationId)) return null;
        String norm = locationId.trim();
        for (Bin b : getAllBins()) {
            if (norm.equalsIgnoreCase(b.getBinId())) return b.getBinId();
        }
        String warehouseId = findWarehouseIdForLocation(norm);
        if (isBlank(warehouseId)) return null;
        BinDAO binDAO = daoFactory.getDAO(DAOFactory.DAOType.BIN);
        List<Bin> bins = binDAO.findByWarehouse(warehouseId);
        if (!bins.isEmpty()) return bins.get(0).getBinId();
        return binDAO.ensureDefaultBinForWarehouse(warehouseId);
    }

    private String findWarehouseIdForLocation(String locationId) {
        if (isBlank(locationId)) return null;
        String norm = locationId.trim();
        for (Warehouse w : getAllWarehouses()) {
            if (norm.equalsIgnoreCase(w.getWarehouseId())
                    || norm.equalsIgnoreCase(w.getWarehouseName())) {
                return w.getWarehouseId();
            }
        }
        return null;
    }

    private Product findProductByIdentifier(String identifier) {
        if (isBlank(identifier)) return null;
        String norm = identifier.trim();
        for (Product p : getAllProducts()) {
            if (norm.equalsIgnoreCase(p.getSku())
                    || norm.equalsIgnoreCase(p.getProductId())
                    || norm.equalsIgnoreCase(p.getProductName())) {
                return p;
            }
        }
        return null;
    }

    private Product ensureProductForStockOperation(String skuOrIdentifier, String supplierId) {
        Product existing = findProductByIdentifier(skuOrIdentifier);
        if (existing != null) return existing;

        Product created = new Product();
        created.setProductId(newProductId());
        created.setProductName(skuOrIdentifier.trim());
        created.setSku(skuOrIdentifier.trim());
        created.setCategory("GENERAL");
        created.setSubCategory("GENERAL");
        created.setSupplierId(isBlank(supplierId) ? "DEFAULT" : supplierId.trim());
        created.setUnitOfMeasure("PCS");
        created.setStorageConditions("N/A");
        created.setShelfLifeDays(0);
        int inserted = createProduct(created);
        return inserted > 0 ? created : null;
    }

    private String resolveSupplierForProduct(Product product, String suppliedSupplierId) {
        if (product != null && !isBlank(product.getSupplierId())) {
            return product.getSupplierId().trim();
        }
        if (!isBlank(suppliedSupplierId)) return suppliedSupplierId.trim();
        return "DEFAULT";
    }

    private String normalizeSupplierForRead(Product product, String supplierId) {
        if (isBlank(supplierId) || "ALL".equalsIgnoreCase(supplierId.trim())) return "ALL";
        return resolveSupplierForProduct(product, supplierId);
    }

    private String canonicalInventorySku(String skuInput, Product product) {
        if (product != null && !isBlank(product.getSku())) return product.getSku().trim();
        return isBlank(skuInput) ? skuInput : skuInput.trim();
    }

    private int quantityFromDbForProduct(String productId) {
        return daoFactory.<StockRecordDAO>getDAO(DAOFactory.DAOType.STOCK_RECORD)
                .sumByProduct(productId);
    }

    private int quantityFromDbForBin(String productId, String binId) {
        return daoFactory.<StockRecordDAO>getDAO(DAOFactory.DAOType.STOCK_RECORD)
                .sumByProductAndBin(productId, binId);
    }

    private int quantityFromDbForWarehouse(String productId, String warehouseId) {
        return daoFactory.<StockRecordDAO>getDAO(DAOFactory.DAOType.STOCK_RECORD)
                .sumByProductAndWarehouse(productId, warehouseId);
    }

    private void syncStockLevelFromRecords(String productId) {
        if (isBlank(productId)) return;
        StockLevelDAO dao = daoFactory.getDAO(DAOFactory.DAOType.STOCK_LEVEL);
        StockLevel level = dao.findByProduct(productId);
        if (level == null) {
            ensureStockLevelExists(productId);
            level = dao.findByProduct(productId);
            if (level == null) return;
        }
        int current = quantityFromDbForProduct(productId);
        level.setCurrentStockQty(Math.max(0, current));
        level.setAvailableStockQty(Math.max(0, level.getCurrentStockQty() - level.getReservedStockQty()));
        dao.update(level);
    }

    private List<InventoryBatch> loadBatchesFromDb(String productId, String supplierId, int totalQtyAtLocation) {
        List<InventoryBatch> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT pb.batch_id, pb.received_date, et.expiry_date " +
                        "FROM product_batches pb " +
                        "LEFT JOIN expiry_tracking et ON et.batch_id = pb.batch_id " +
                        "WHERE pb.product_id = ? "
        );
        boolean hasSupplierFilter = !isBlank(supplierId) && !"ALL".equalsIgnoreCase(supplierId.trim());
        if (hasSupplierFilter) {
            sql.append("AND pb.supplier_id = ? ");
        }
        sql.append("ORDER BY pb.received_date DESC");

        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, productId);
            if (hasSupplierFilter) {
                ps.setString(2, supplierId.trim());
            }

            List<String> batchIds = new ArrayList<>();
            List<LocalDateTime> arrivals = new ArrayList<>();
            List<LocalDateTime> expiries = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    batchIds.add(rs.getString("batch_id"));
                    Timestamp receivedTs = rs.getTimestamp("received_date");
                    Date expiryDate = rs.getDate("expiry_date");
                    arrivals.add(receivedTs == null ? LocalDateTime.now() : receivedTs.toLocalDateTime());
                    expiries.add(expiryDate == null ? null : expiryDate.toLocalDate().atStartOfDay());
                }
            }

            if (batchIds.isEmpty()) return result;
            int count = batchIds.size();
            int perBatch = totalQtyAtLocation / count;
            int remainder = totalQtyAtLocation % count;
            for (int i = 0; i < count; i++) {
                int qty = perBatch + (i < remainder ? 1 : 0);
                result.add(new InventoryBatch(
                        batchIds.get(i),
                        Math.max(0, qty),
                        arrivals.get(i),
                        expiries.get(i),
                        new ArrayList<>(),
                        0.0
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Inventory] loadBatchesFromDb error: " + e.getMessage());
        }
        return result;
    }

    private void ensureInventoryMetadataTables() {
        if (inventoryMetadataTablesReady) return;
        synchronized (this) {
            if (inventoryMetadataTablesReady) return;
            try (Connection conn = DatabaseConnectionPool.getInstance().getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(
                        "CREATE TABLE IF NOT EXISTS inventory_suppliers (" +
                                "supplier_id VARCHAR(50) NOT NULL PRIMARY KEY, " +
                                "name VARCHAR(150) NOT NULL, " +
                                "lead_time_days INT NOT NULL DEFAULT 0, " +
                                "performance_rating DOUBLE NOT NULL DEFAULT 0, " +
                                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                                ")"
                );
                st.execute(
                        "CREATE TABLE IF NOT EXISTS inventory_settings (" +
                                "setting_key VARCHAR(80) NOT NULL PRIMARY KEY, " +
                                "setting_value VARCHAR(120) NOT NULL, " +
                                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                                ")"
                );
                inventoryMetadataTablesReady = true;
            } catch (SQLException e) {
                System.err.println("[Inventory] ensureInventoryMetadataTables error: " + e.getMessage());
            }
        }
    }

    private String readInventorySetting(String key) {
        if (isBlank(key)) return null;
        ensureInventoryMetadataTables();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT setting_value FROM inventory_settings WHERE setting_key=?"
             )) {
            ps.setString(1, key.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("setting_value") : null;
            }
        } catch (SQLException e) {
            System.err.println("[Inventory] readInventorySetting error: " + e.getMessage());
            return null;
        }
    }

    private void writeInventorySetting(String key, String value) {
        if (isBlank(key) || isBlank(value)) return;
        ensureInventoryMetadataTables();
        try (Connection conn = DatabaseConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO inventory_settings(setting_key, setting_value) VALUES(?,?) " +
                             "ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value), updated_at=CURRENT_TIMESTAMP"
             )) {
            ps.setString(1, key.trim());
            ps.setString(2, value.trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Inventory] writeInventorySetting error: " + e.getMessage());
        }
    }

    private void applyDbIssuingPolicyToRuntime() {
        String policy = inventoryGetIssuingPolicy();
        if (!(inventoryService instanceof InventoryService concrete)) return;
        try {
            concrete.setIssuingPolicy(IssuingPolicy.valueOf(policy));
        } catch (Exception ignored) {
            concrete.setIssuingPolicy(IssuingPolicy.FIFO);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ── ID generators ────────────────────────────────────────────────────────

    private String newStockLevelId() {
        return "STK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String newStockRecordId() {
        return "SR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String newStockMovementId() {
        return "SM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String newProductId() {
        return "PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
