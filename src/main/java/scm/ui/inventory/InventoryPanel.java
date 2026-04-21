package scm.ui.inventory;

import inventory_subsystem.InventoryBatch;
import inventory_subsystem.Supplier;
import inventory_subsystem.StockTransaction;
import scm.ui.model.*;
import scm.ui.patterns.BarcodeReaderAdapter;
import scm.ui.patterns.EventBus;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * C-04 — Inventory & Warehouse UI Panel
 * Tabs: Products | Stock Levels | Stock Movements | Warehouses & Bins | Barcode/RFID
 *       | Stock Operations  (Inventory Subsystem integration)
 *
 * Inputs:  productList, productSKU, currentStockLevel, reorderThreshold,
 *          barcodeRFIDValue, warehouseZoneData, stockTransferRequest
 * Outputs: paginated stock table, low-stock alerts, add/edit form, stock transfer request
 *
 * INVENTORY SUBSYSTEM INTEGRATION — UPDATED (Integration Spec UPDATED — §3):
 *   The "Stock Operations" tab exposes addStock, removeStock, transferStock,
 *   and getStock directly via the InventoryUI interface (through SupplyChainFacade).
 *
 *   UPDATED: All four operations now require a Supplier ID field, matching the
 *   updated InventoryUI interface signature:
 *       addStock      (sku, locationId, supplierId, quantity)
 *       removeStock   (sku, locationId, supplierId, quantity)
 *       transferStock (sku, fromLocation, toLocation, supplierId, quantity)
 *       getStock      (sku, locationId, supplierId)
 *
 *   This panel does NOT implement any business logic — all validation and
 *   exception-firing is handled by the inventory subsystem.
 */
public class InventoryPanel extends JPanel {

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();
    private JTabbedPane tabs;

    // ── Sub-panels ───────────────────────────────────────────────────────────
    private ProductCrudPanel           productPanel;
    private StockLevelCrudPanel        stockPanel;
    private StockMovementPanel         movementPanel;
    private WarehouseBinPanel          warehousePanel;
    private WarehouseProductStockPanel warehouseProductStockPanel;
    private BarcodePanel               barcodePanel;
    private StockOperationsPanel       stockOpsPanel;   // ← Inventory Subsystem

    public InventoryPanel() {
        setLayout(new BorderLayout());
        setBackground(LuxuryTheme.BG_PANEL);
        buildUI();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LuxuryTheme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 0, 28));
        header.add(LuxuryTheme.sectionTitle("Inventory  &  Warehouse"), BorderLayout.WEST);

        tabs = new JTabbedPane();
        tabs.setBackground(LuxuryTheme.BG_PANEL);
        tabs.setForeground(LuxuryTheme.TEXT_SECOND);
        tabs.setFont(LuxuryTheme.FONT_SUBHEAD);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        productPanel               = new ProductCrudPanel();
        stockPanel                 = new StockLevelCrudPanel();
        movementPanel              = new StockMovementPanel();
        warehousePanel             = new WarehouseBinPanel();
        warehouseProductStockPanel = new WarehouseProductStockPanel();
        barcodePanel               = new BarcodePanel();
        stockOpsPanel              = new StockOperationsPanel();

        tabs.addTab("📦 Products",           productPanel);
        tabs.addTab("📊 Stock Levels",       stockPanel);
        tabs.addTab("🔄 Movements",          movementPanel);
        tabs.addTab("🏭 Warehouses & Bins",  warehousePanel);
        tabs.addTab("📋 Warehouse Product Stock", warehouseProductStockPanel);
        tabs.addTab("📡 Barcode / RFID",     barcodePanel);
        tabs.addTab("🔧 Stock Operations",   stockOpsPanel);

        EventBus.getInstance().subscribe(EventBus.Event.STOCK_CHANGED, payload ->
            SwingUtilities.invokeLater(() -> {
                stockPanel.refreshData();
                movementPanel.refreshData();
                warehouseProductStockPanel.refreshData();
                warehousePanel.refreshData();
            })
        );
        EventBus.getInstance().subscribe(EventBus.Event.PRODUCT_CREATED, payload ->
            SwingUtilities.invokeLater(() -> {
                productPanel.refreshData();
                stockPanel.refreshData();
                warehouseProductStockPanel.refreshData();
            })
        );
        EventBus.getInstance().subscribe(EventBus.Event.PRODUCT_UPDATED, payload ->
            SwingUtilities.invokeLater(() -> {
                productPanel.refreshData();
                warehouseProductStockPanel.refreshData();
            })
        );

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Products CRUD
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class ProductCrudPanel extends CrudPanel {
        ProductCrudPanel() {
            init("Products", new String[]{
                "Product ID","Name","SKU","Category","Sub-Category","Supplier","UOM","Shelf Life"
            });
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            List<Product> products = facade.getAllProducts();
            SwingUtilities.invokeLater(() -> {
                for (Product p : products) {
                    tableModel.addRow(new Object[]{
                        p.getProductId(), p.getProductName(), p.getSku(),
                        p.getCategory(), p.getSubCategory(), p.getSupplierId(),
                        p.getUnitOfMeasure(), p.getShelfLifeDays()
                    });
                }
                updateRecordCount();
            });
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Add Product", 460, 460);
            JTextField idF    = LuxuryTheme.textField(AppUtils.newId("PROD"));
            JTextField nameF  = LuxuryTheme.textField("Product Name");
            JTextField skuF   = LuxuryTheme.textField("SKU-001");
            JTextField catF   = LuxuryTheme.textField("Category");
            JTextField subF   = LuxuryTheme.textField("Sub-Category");
            JTextField supF   = LuxuryTheme.textField("SUP-001");
            JTextField uomF   = LuxuryTheme.textField("UNIT");
            JSpinner   shelfSp = LuxuryTheme.spinner(0, 3650, 0);
            JPanel form = new FormBuilder()
                .addField("Product ID *", idF).addField("Name *", nameF)
                .addField("SKU *", skuF).addField("Category", catF)
                .addField("Sub-Category", subF).addField("Supplier ID", supF)
                .addField("UOM", uomF).addField("Shelf Life (days)", shelfSp)
                .build();
            addDialogButtons(d, form, () -> {
                Product p = new Product();
                p.setProductId(idF.getText().trim()); p.setProductName(nameF.getText().trim());
                p.setSku(skuF.getText().trim());      p.setCategory(catF.getText().trim());
                p.setSubCategory(subF.getText().trim()); p.setSupplierId(supF.getText().trim());
                p.setUnitOfMeasure(uomF.getText().trim());
                p.setShelfLifeDays((int) shelfSp.getValue());
                int r = facade.createProduct(p);
                if (r > 0) { d.dispose(); loadDataAsync(); }
                else AppUtils.showError(d, "Failed to create product.");
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            JDialog d = createDialog("Edit Product", 460, 460);
            JTextField idF    = LuxuryTheme.textField((String) tableModel.getValueAt(row, 0));
            JTextField nameF  = LuxuryTheme.textField((String) tableModel.getValueAt(row, 1));
            JTextField skuF   = LuxuryTheme.textField((String) tableModel.getValueAt(row, 2));
            JTextField catF   = LuxuryTheme.textField((String) tableModel.getValueAt(row, 3));
            JTextField subF   = LuxuryTheme.textField((String) tableModel.getValueAt(row, 4));
            JTextField supF   = LuxuryTheme.textField((String) tableModel.getValueAt(row, 5));
            JTextField uomF   = LuxuryTheme.textField((String) tableModel.getValueAt(row, 6));
            Object shelfVal = tableModel.getValueAt(row, 7);
            int shelfDays = shelfVal instanceof Integer ? (Integer) shelfVal : 0;
            JSpinner shelfSp  = LuxuryTheme.spinner(0, 3650, shelfDays);
            idF.setEditable(false);
            JPanel form = new FormBuilder()
                .addField("Product ID", idF).addField("Name *", nameF)
                .addField("SKU *", skuF).addField("Category", catF)
                .addField("Sub-Category", subF).addField("Supplier ID", supF)
                .addField("UOM", uomF).addField("Shelf Life (days)", shelfSp)
                .build();
            addDialogButtons(d, form, () -> {
                Product p = new Product();
                p.setProductId(id); p.setProductName(nameF.getText().trim());
                p.setSku(skuF.getText().trim()); p.setCategory(catF.getText().trim());
                p.setSubCategory(subF.getText().trim()); p.setSupplierId(supF.getText().trim());
                p.setUnitOfMeasure(uomF.getText().trim());
                p.setShelfLifeDays((int) shelfSp.getValue());
                int r = facade.updateProduct(p);
                if (r > 0) { d.dispose(); loadDataAsync(); }
                else AppUtils.showError(d, "Failed to update product.");
            });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            facade.deleteProduct(id); loadDataAsync();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Stock Levels CRUD
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class StockLevelCrudPanel extends CrudPanel {
        StockLevelCrudPanel() {
            init("Stock Levels", new String[]{
                "Stock ID","Product ID","Current Qty","Reserved Qty",
                "Available Qty","Reorder Threshold","Reorder Qty","Safety Stock"
            });
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            List<StockLevel> levels = facade.getAllStockLevels();
            SwingUtilities.invokeLater(() -> {
                for (StockLevel s : levels) {
                    boolean low = s.getCurrentStockQty() < s.getReorderThreshold();
                    tableModel.addRow(new Object[]{
                        low ? "⚠ " + s.getStockLevelId() : s.getStockLevelId(),
                        s.getProductId(),
                        s.getCurrentStockQty(), s.getReservedStockQty(),
                        s.getAvailableStockQty(), s.getReorderThreshold(),
                        s.getReorderQuantity(), s.getSafetyStockLevel()
                    });
                }
                updateRecordCount();
            });
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Add Stock Level", 460, 420);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("STK"));
            JTextField pidF  = LuxuryTheme.textField("PROD-001");
            JSpinner curSp   = LuxuryTheme.spinner(0, 999999, 0);
            JSpinner resSp   = LuxuryTheme.spinner(0, 999999, 0);
            JSpinner avlSp   = LuxuryTheme.spinner(0, 999999, 0);
            JSpinner reoThSp = LuxuryTheme.spinner(0, 999999, 10);
            JSpinner reoQSp  = LuxuryTheme.spinner(0, 999999, 50);
            JSpinner safeSp  = LuxuryTheme.spinner(0, 999999, 5);
            JPanel form = new FormBuilder()
                .addField("Stock Level ID *", idF).addField("Product ID *", pidF)
                .addField("Current Qty", curSp).addField("Reserved Qty", resSp)
                .addField("Available Qty", avlSp).addField("Reorder Threshold", reoThSp)
                .addField("Reorder Qty", reoQSp).addField("Safety Stock", safeSp)
                .build();
            addDialogButtons(d, form, () -> {
                StockLevel s = new StockLevel();
                s.setStockLevelId(idF.getText().trim()); s.setProductId(pidF.getText().trim());
                s.setCurrentStockQty((int) curSp.getValue());
                s.setReservedStockQty((int) resSp.getValue());
                s.setAvailableStockQty((int) avlSp.getValue());
                s.setReorderThreshold((int) reoThSp.getValue());
                s.setReorderQuantity((int) reoQSp.getValue());
                s.setSafetyStockLevel((int) safeSp.getValue());
                int r = facade.createStockLevel(s);
                if (r > 0) { d.dispose(); loadDataAsync(); }
                else AppUtils.showError(d, "Failed to create stock level.");
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = ((String) tableModel.getValueAt(row, 0)).replace("⚠ ", "");
            JDialog d = createDialog("Edit Stock Level", 460, 420);
            JTextField idF   = LuxuryTheme.textField(id);
            JTextField pidF  = LuxuryTheme.textField((String) tableModel.getValueAt(row, 1));
            JSpinner curSp   = LuxuryTheme.spinner(0, 999999, tableModel.getValueAt(row, 2));
            JSpinner resSp   = LuxuryTheme.spinner(0, 999999, tableModel.getValueAt(row, 3));
            JSpinner avlSp   = LuxuryTheme.spinner(0, 999999, tableModel.getValueAt(row, 4));
            JSpinner reoThSp = LuxuryTheme.spinner(0, 999999, tableModel.getValueAt(row, 5));
            JSpinner reoQSp  = LuxuryTheme.spinner(0, 999999, tableModel.getValueAt(row, 6));
            JSpinner safeSp  = LuxuryTheme.spinner(0, 999999, tableModel.getValueAt(row, 7));
            idF.setEditable(false);
            JPanel form = new FormBuilder()
                .addField("Stock Level ID", idF).addField("Product ID *", pidF)
                .addField("Current Qty", curSp).addField("Reserved Qty", resSp)
                .addField("Available Qty", avlSp).addField("Reorder Threshold", reoThSp)
                .addField("Reorder Qty", reoQSp).addField("Safety Stock", safeSp)
                .build();
            addDialogButtons(d, form, () -> {
                StockLevel s = new StockLevel();
                s.setStockLevelId(id); s.setProductId(pidF.getText().trim());
                s.setCurrentStockQty((int) curSp.getValue());
                s.setReservedStockQty((int) resSp.getValue());
                s.setAvailableStockQty((int) avlSp.getValue());
                s.setReorderThreshold((int) reoThSp.getValue());
                s.setReorderQuantity((int) reoQSp.getValue());
                s.setSafetyStockLevel((int) safeSp.getValue());
                int r = facade.updateStockLevel(s);
                if (r > 0) { d.dispose(); loadDataAsync(); }
                else AppUtils.showError(d, "Failed to update stock level.");
            });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) {
            String id = ((String) tableModel.getValueAt(row, 0)).replace("⚠ ", "");
            facade.deleteStockLevel(id); loadDataAsync();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Stock Movements (read-only view + delete)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class StockMovementPanel extends CrudPanel {
        StockMovementPanel() {
            init("Stock Movements", new String[]{
                "Movement ID","Type","From Bin","To Bin","Product ID","Qty"
            });
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            List<StockMovement> movements = facade.getAllStockMovements();
            SwingUtilities.invokeLater(() -> {
                for (StockMovement m : movements) {
                    tableModel.addRow(new Object[]{
                        m.getMovementId(), m.getMovementType(),
                        m.getFromBin(), m.getToBin(),
                        m.getProductId(), m.getMovedQty()
                    });
                }
                updateRecordCount();
            });
        }

        @Override protected void openAddDialog()           { /* driven by operations */ }
        @Override protected void openEditDialog(int row)   { /* read-only */ }
        @Override protected void deleteRow(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            facade.deleteStockMovement(id); loadDataAsync();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Warehouses & Bins CRUD
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class WarehouseBinPanel extends JPanel {
        private final JTabbedPane inner;
        WarehouseBinPanel() {
            setLayout(new BorderLayout());
            setBackground(LuxuryTheme.BG_PANEL);
            inner = new JTabbedPane();
            inner.addTab("Warehouses", new WarehouseCrudPanel());
            inner.addTab("Bins",       new BinCrudPanel());
            add(inner, BorderLayout.CENTER);
        }
        void refreshData() {
            for (int i = 0; i < inner.getTabCount(); i++) {
                if (inner.getComponentAt(i) instanceof CrudPanel cp) cp.refreshData();
            }
        }
    }

    class WarehouseCrudPanel extends CrudPanel {
        WarehouseCrudPanel() {
            init("Warehouses", new String[]{"Warehouse ID","Name","Location","Capacity"});
        }
        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            List<Warehouse> warehouses = facade.getAllWarehouses();
            SwingUtilities.invokeLater(() -> {
                for (Warehouse w : warehouses) {
                    tableModel.addRow(new Object[]{
                        w.getWarehouseId(), w.getWarehouseName(),
                        w.getLocation(), w.getCapacity()
                    });
                }
                updateRecordCount();
            });
        }
        @Override protected void openAddDialog() {
            JDialog d = createDialog("Add Warehouse", 420, 300);
            JTextField idF  = LuxuryTheme.textField(AppUtils.newId("WH"));
            JTextField nmF  = LuxuryTheme.textField("Warehouse Name");
            JTextField locF = LuxuryTheme.textField("City, Country");
            JSpinner   capSp = LuxuryTheme.spinner(0, 9999999, 1000);
            JPanel form = new FormBuilder()
                .addField("Warehouse ID *", idF).addField("Name *", nmF)
                .addField("Location", locF).addField("Capacity", capSp).build();
            addDialogButtons(d, form, () -> {
                Warehouse w = new Warehouse();
                w.setWarehouseId(idF.getText().trim()); w.setWarehouseName(nmF.getText().trim());
                w.setLocation(locF.getText().trim()); w.setCapacity((int) capSp.getValue());
                int r = facade.createWarehouse(w);
                if (r > 0) { d.dispose(); loadDataAsync(); }
                else AppUtils.showError(d, "Failed to create warehouse.");
            });
            d.setVisible(true);
        }
        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            JDialog d = createDialog("Edit Warehouse", 420, 300);
            JTextField idF  = LuxuryTheme.textField(id);
            JTextField nmF  = LuxuryTheme.textField((String) tableModel.getValueAt(row, 1));
            JTextField locF = LuxuryTheme.textField((String) tableModel.getValueAt(row, 2));
            JSpinner   capSp = LuxuryTheme.spinner(0, 9999999, tableModel.getValueAt(row, 3));
            idF.setEditable(false);
            JPanel form = new FormBuilder()
                .addField("Warehouse ID", idF).addField("Name *", nmF)
                .addField("Location", locF).addField("Capacity", capSp).build();
            addDialogButtons(d, form, () -> {
                Warehouse w = new Warehouse();
                w.setWarehouseId(id); w.setWarehouseName(nmF.getText().trim());
                w.setLocation(locF.getText().trim()); w.setCapacity((int) capSp.getValue());
                int r = facade.updateWarehouse(w);
                if (r > 0) { d.dispose(); loadDataAsync(); }
                else AppUtils.showError(d, "Failed to update warehouse.");
            });
            d.setVisible(true);
        }
        @Override protected void deleteRow(int row) {
            facade.deleteWarehouse((String) tableModel.getValueAt(row, 0)); loadDataAsync();
        }
    }

    class BinCrudPanel extends CrudPanel {
        BinCrudPanel() {
            init("Bins", new String[]{"Bin ID","Warehouse ID","Zone","Capacity"});
        }
        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            List<Bin> bins = facade.getAllBins();
            SwingUtilities.invokeLater(() -> {
                for (Bin b : bins) {
                    tableModel.addRow(new Object[]{
                        b.getBinId(), b.getWarehouseId(), b.getZone(), b.getCapacity()
                    });
                }
                updateRecordCount();
            });
        }
        @Override protected void openAddDialog() {
            JDialog d = createDialog("Add Bin", 420, 280);
            JTextField idF  = LuxuryTheme.textField(AppUtils.newId("BIN"));
            JTextField whF  = LuxuryTheme.textField("WH-001");
            JTextField znF  = LuxuryTheme.textField("A");
            JSpinner   capSp = LuxuryTheme.spinner(0, 99999, 100);
            JPanel form = new FormBuilder()
                .addField("Bin ID *", idF).addField("Warehouse ID *", whF)
                .addField("Zone", znF).addField("Capacity", capSp).build();
            addDialogButtons(d, form, () -> {
                Bin b = new Bin();
                b.setBinId(idF.getText().trim()); b.setWarehouseId(whF.getText().trim());
                b.setZone(znF.getText().trim()); b.setCapacity((int) capSp.getValue());
                int r = facade.createBin(b);
                if (r > 0) { d.dispose(); loadDataAsync(); }
                else AppUtils.showError(d, "Failed to create bin.");
            });
            d.setVisible(true);
        }
        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            JDialog d = createDialog("Edit Bin", 420, 280);
            JTextField idF  = LuxuryTheme.textField(id);
            JTextField whF  = LuxuryTheme.textField((String) tableModel.getValueAt(row, 1));
            JTextField znF  = LuxuryTheme.textField((String) tableModel.getValueAt(row, 2));
            JSpinner   capSp = LuxuryTheme.spinner(0, 99999, tableModel.getValueAt(row, 3));
            idF.setEditable(false);
            JPanel form = new FormBuilder()
                .addField("Bin ID", idF).addField("Warehouse ID *", whF)
                .addField("Zone", znF).addField("Capacity", capSp).build();
            addDialogButtons(d, form, () -> {
                Bin b = new Bin();
                b.setBinId(id); b.setWarehouseId(whF.getText().trim());
                b.setZone(znF.getText().trim()); b.setCapacity((int) capSp.getValue());
                int r = facade.updateBin(b);
                if (r > 0) { d.dispose(); loadDataAsync(); }
                else AppUtils.showError(d, "Failed to update bin.");
            });
            d.setVisible(true);
        }
        @Override protected void deleteRow(int row) {
            facade.deleteBin((String) tableModel.getValueAt(row, 0)); loadDataAsync();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Warehouse Product Stock (read-only view)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class WarehouseProductStockPanel extends CrudPanel {
        WarehouseProductStockPanel() {
            init("Warehouse Product Stock", new String[]{
                "Product ID","Product Name","Bin ID","Warehouse ID","Quantity"
            });
        }
        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            List<WarehouseProductStock> rows = facade.getWarehouseProductStocks();
            SwingUtilities.invokeLater(() -> {
                for (WarehouseProductStock r : rows) {
                    tableModel.addRow(new Object[]{
                        r.getProductId(), r.getProductName(),
                        r.getBinId(), r.getWarehouseId(), r.getQuantity()
                    });
                }
                updateRecordCount();
            });
        }
        @Override protected void openAddDialog()         { /* read-only */ }
        @Override protected void openEditDialog(int row) { /* read-only */ }
        @Override protected void deleteRow(int row)      { /* read-only */ }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Barcode / RFID Panel
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class BarcodePanel extends CrudPanel {
        private final BarcodeReaderAdapter barcodeAdapter =
            new BarcodeReaderAdapter(new BarcodeReaderAdapter.SimulatedScanner());

        BarcodePanel() {
            init("Barcode / RFID Events", new String[]{
                "Event ID","SKU","Type","Quantity","Location","Timestamp"
            });
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            List<BarcodeEvent> events = facade.getAllBarcodeEvents();
            SwingUtilities.invokeLater(() -> {
                for (BarcodeEvent e : events) {
                    tableModel.addRow(new Object[]{
                        e.getEventId(), e.getSku(), e.getEventType(),
                        e.getQuantity(), e.getLocationId(), e.getTimestamp()
                    });
                }
                updateRecordCount();
            });
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Scan Barcode / RFID", 420, 300);
            JTextField skuF  = LuxuryTheme.textField("SKU or Barcode");
            JTextField locF  = LuxuryTheme.textField("Location ID");
            JSpinner   qtySp = LuxuryTheme.spinner(1, 99999, 1);
            String[] types = {"INBOUND", "OUTBOUND", "TRANSFER", "AUDIT"};
            JComboBox<String> typeBox = new JComboBox<>(types);
            JPanel form = new FormBuilder()
                .addField("SKU / Barcode *", skuF).addField("Location *", locF)
                .addField("Quantity", qtySp).addField("Event Type", typeBox).build();
            addDialogButtons(d, form, () -> {
                BarcodeEvent ev = barcodeAdapter.scan(
                    skuF.getText().trim(), locF.getText().trim(),
                    (int) qtySp.getValue(), (String) typeBox.getSelectedItem());
                int r = facade.createBarcodeEvent(ev);
                if (r > 0) { d.dispose(); loadDataAsync(); }
                else AppUtils.showError(d, "Failed to record barcode event.");
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) { /* events are immutable */ }
        @Override protected void deleteRow(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            facade.deleteBarcodeEvent(id); loadDataAsync();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Stock Operations — Inventory Subsystem Integration (C-04)
    //
    // Wires directly to InventoryUI through SupplyChainFacade.
    //
    // Per Integration Spec UPDATED — §3 (UI Team):
    //   • UI calls addStock / removeStock / transferStock / getStock
    //   • ALL operations now include supplierId as a required field
    //   • UI must NOT implement business logic
    //   • UI must NOT handle exceptions (they are fired to exception source)
    //   • UI must NOT access InventoryRepository directly
    //
    // Exception codes the subsystem may fire (displayed via SCMExceptionHandler):
    //   166 ITEM_NOT_FOUND       — SKU/location/supplier not in inventory
    //   167 INSUFFICIENT_STOCK   — not enough quantity to remove/transfer
    //   110 STOCK_UPDATE_CONFLICT — concurrent modification detected
    //   200 REORDER_TRIGGER       — stock fell below reorder threshold
    //   201 SAFETY_STOCK_BREACH   — stock fell below safety stock level
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class StockOperationsPanel extends JPanel {

        private final JTextArea   logArea;
        private final JScrollPane logScroll;

        StockOperationsPanel() {
            setLayout(new BorderLayout(0, 0));
            setBackground(LuxuryTheme.BG_PANEL);
            setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

            JLabel title = new JLabel("Inventory Subsystem — Stock Operations  (UPDATED)");
            title.setFont(LuxuryTheme.FONT_SUBHEAD);
            title.setForeground(LuxuryTheme.TEXT_GOLD);
            title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

            // 8 operation cards laid out in 2 columns to avoid horizontal clipping.
            JPanel cardsRow = new JPanel(new GridLayout(0, 2, 12, 12));
            cardsRow.setBackground(LuxuryTheme.BG_PANEL);
            cardsRow.add(buildAddStockCard());
            cardsRow.add(buildRemoveStockCard());
            cardsRow.add(buildTransferStockCard());
            cardsRow.add(buildGetStockCard());
            cardsRow.add(buildSupplierCard());
            cardsRow.add(buildPolicyCard());
            cardsRow.add(buildBatchViewCard());
            cardsRow.add(buildTransactionViewCard());

            logArea = new JTextArea(8, 60);
            logArea.setEditable(false);
            logArea.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
            logArea.setBackground(LuxuryTheme.BG_DEEP);
            logArea.setForeground(LuxuryTheme.TEXT_PRIMARY);
            logArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            logArea.setText("Operation log — results appear here.\n");
            logScroll = new JScrollPane(logArea);
            logScroll.setBorder(new LineBorder(LuxuryTheme.BG_CARD, 1));
            logScroll.setPreferredSize(new Dimension(0, 220));
            logScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
            LuxuryTheme.styleScrollBar(logScroll.getVerticalScrollBar());

            JLabel logLabel = new JLabel("  Operation Log");
            logLabel.setFont(LuxuryTheme.FONT_BODY);
            logLabel.setForeground(LuxuryTheme.TEXT_MUTED);
            logLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));

            JPanel north = new JPanel(new BorderLayout());
            north.setBackground(LuxuryTheme.BG_PANEL);
            north.add(title,    BorderLayout.NORTH);
            north.add(cardsRow, BorderLayout.CENTER);
            north.add(logLabel, BorderLayout.SOUTH);

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(LuxuryTheme.BG_PANEL);

            north.setAlignmentX(Component.LEFT_ALIGNMENT);
            logScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(north);
            content.add(Box.createVerticalStrut(8));
            content.add(logScroll);

            JScrollPane containerScroll = new JScrollPane(content);
            containerScroll.setBorder(BorderFactory.createEmptyBorder());
            containerScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            containerScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            containerScroll.getVerticalScrollBar().setUnitIncrement(16);
            LuxuryTheme.styleScrollBar(containerScroll.getVerticalScrollBar());

            add(containerScroll, BorderLayout.CENTER);
        }

        private void log(String msg) {
            SwingUtilities.invokeLater(() -> {
                logArea.append("[" + java.time.LocalTime.now().toString().substring(0, 8)
                        + "]  " + msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }

        // ── Add Stock card ────────────────────────────────────────────────────
        // Integration Spec §4.1 — Used by: Warehouse Management, Barcode/RFID Tracker
        // UPDATED: supplierId field added — creates batch and stores supplier link
        private JPanel buildAddStockCard() {
            JPanel card = buildCard("➕ Add Stock", LuxuryTheme.ACCENT_GOLD);

            JTextField skuF  = LuxuryTheme.textField("SKU or Product ID");
            JTextField locF  = LuxuryTheme.textField("Bin ID or Warehouse ID");
            JTextField supF  = LuxuryTheme.textField("Supplier ID");
            JSpinner   qtySp = LuxuryTheme.spinner(1, 999999, 1);

            JButton btn = LuxuryTheme.primaryButton("Add Stock");
            btn.addActionListener(e -> {
                String sku = skuF.getText().trim();
                String loc = locF.getText().trim();
                String sup = supF.getText().trim();
                int    qty = (int) qtySp.getValue();
                if (sku.isEmpty() || loc.isEmpty() || sup.isEmpty()) {
                    AppUtils.showError(this, "SKU/Product ID, Location, and Supplier ID are required.");
                    return;
                }
                // Delegate to InventoryUI via facade — no business logic here
                facade.inventoryAddStock(sku, loc, sup, qty);
                log("addStock  item=" + sku + "  location=" + loc
                        + "  supplier=" + sup + "  qty=+" + qty + "  → OK");
            });

            card.add(fieldRow("SKU / Product ID *", skuF));
            card.add(fieldRow("Location *",    locF));
            card.add(fieldRow("Supplier ID *", supF));
            card.add(fieldRow("Quantity",      qtySp));
            card.add(Box.createVerticalStrut(8));
            card.add(btn);
            return card;
        }

        // ── Remove Stock card ─────────────────────────────────────────────────
        // Integration Spec §4.2 — Used by: Order Fulfillment
        // UPDATED: supplierId field added — FIFO/FEFO applied per supplier batch
        private JPanel buildRemoveStockCard() {
            JPanel card = buildCard("➖ Remove Stock", new Color(0xC0392B));

            JTextField skuF  = LuxuryTheme.textField("SKU or Product ID");
            JTextField locF  = LuxuryTheme.textField("Bin ID or Warehouse ID");
            JTextField supF  = LuxuryTheme.textField("Supplier ID");
            JSpinner   qtySp = LuxuryTheme.spinner(1, 999999, 1);

            JButton btn = LuxuryTheme.primaryButton("Remove Stock");
            btn.addActionListener(e -> {
                String sku = skuF.getText().trim();
                String loc = locF.getText().trim();
                String sup = supF.getText().trim();
                int    qty = (int) qtySp.getValue();
                if (sku.isEmpty() || loc.isEmpty() || sup.isEmpty()) {
                    AppUtils.showError(this, "SKU/Product ID, Location, and Supplier ID are required.");
                    return;
                }
                // Delegate to InventoryUI via facade — exceptions (166, 167, 110)
                // are fired to the registered SCMExceptionHandler; UI does not handle them.
                facade.inventoryRemoveStock(sku, loc, sup, qty);
                log("removeStock  item=" + sku + "  location=" + loc
                        + "  supplier=" + sup + "  qty=-" + qty + "  → OK");
            });

            card.add(fieldRow("SKU / Product ID *", skuF));
            card.add(fieldRow("Location *",    locF));
            card.add(fieldRow("Supplier ID *", supF));
            card.add(fieldRow("Quantity",      qtySp));
            card.add(Box.createVerticalStrut(8));
            card.add(btn);
            return card;
        }

        // ── Transfer Stock card ───────────────────────────────────────────────
        // Integration Spec §4.3 — Used by: Warehouse Management
        // UPDATED: supplierId field added — maintained on source and destination batch
        private JPanel buildTransferStockCard() {
            JPanel card = buildCard("🔁 Transfer Stock", new Color(0x2980B9));

            JTextField skuF  = LuxuryTheme.textField("SKU or Product ID");
            JTextField fromF = LuxuryTheme.textField("From Bin or Warehouse");
            JTextField toF   = LuxuryTheme.textField("To Bin or Warehouse");
            JTextField supF  = LuxuryTheme.textField("Supplier ID");
            JSpinner   qtySp = LuxuryTheme.spinner(1, 999999, 1);

            JButton btn = LuxuryTheme.primaryButton("Transfer");
            btn.addActionListener(e -> {
                String sku  = skuF.getText().trim();
                String from = fromF.getText().trim();
                String to   = toF.getText().trim();
                String sup  = supF.getText().trim();
                int    qty  = (int) qtySp.getValue();
                if (sku.isEmpty() || from.isEmpty() || to.isEmpty() || sup.isEmpty()) {
                    AppUtils.showError(this,
                        "SKU/Product ID, From, To, and Supplier ID are required.");
                    return;
                }
                // Delegate to InventoryUI via facade — exceptions (166, 167) fired on failure
                facade.inventoryTransferStock(sku, from, to, sup, qty);
                log("transferStock  item=" + sku + "  from=" + from + "  to=" + to
                        + "  supplier=" + sup + "  qty=" + qty + "  → OK");
            });

            card.add(fieldRow("SKU / Product ID *", skuF));
            card.add(fieldRow("From *",        fromF));
            card.add(fieldRow("To *",          toF));
            card.add(fieldRow("Supplier ID *", supF));
            card.add(fieldRow("Quantity",      qtySp));
            card.add(Box.createVerticalStrut(8));
            card.add(btn);
            return card;
        }

        // ── Get Stock card ────────────────────────────────────────────────────
        // Integration Spec §4.4 — Used by: Order Fulfillment, Reporting & Analytics
        // UPDATED: supplierId field added — returns total quantity across batches
        //          for the given SKU / location / supplier triple.
        //          Enter "ALL" or leave blank to sum across all suppliers.
        private JPanel buildGetStockCard() {
            JPanel card = buildCard("🔍 Get Stock", new Color(0x27AE60));

            JTextField skuF   = LuxuryTheme.textField("SKU or Product ID");
            JTextField locF   = LuxuryTheme.textField("Bin ID or Warehouse ID");
            JTextField supF   = LuxuryTheme.textField("Supplier ID  (or ALL)");
            JLabel     result = new JLabel("—");
            result.setFont(LuxuryTheme.FONT_SUBHEAD);
            result.setForeground(LuxuryTheme.ACCENT_GOLD);
            result.setHorizontalAlignment(SwingConstants.CENTER);

            JButton btn = LuxuryTheme.primaryButton("Get Stock");
            btn.addActionListener(e -> {
                String sku = skuF.getText().trim();
                String loc = locF.getText().trim();
                String sup = supF.getText().trim();
                if (sku.isEmpty() || loc.isEmpty()) {
                    AppUtils.showError(this, "SKU/Product ID and Location are required.");
                    return;
                }
                // supplierId blank or "ALL" → facade sums all suppliers at this location
                int qty = facade.inventoryGetStock(sku, loc, sup);
                result.setText(String.valueOf(qty));
                log("getStock  item=" + sku + "  location=" + loc
                        + "  supplier=" + (sup.isEmpty() ? "ALL" : sup)
                        + "  → qty=" + qty);
            });

            JPanel resultRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            resultRow.setBackground(LuxuryTheme.BG_CARD);
            resultRow.add(new JLabel("Qty: ") {{
                setForeground(LuxuryTheme.TEXT_MUTED);
                setFont(LuxuryTheme.FONT_BODY);
            }});
            resultRow.add(result);

            card.add(fieldRow("SKU / Product ID *", skuF));
            card.add(fieldRow("Location *",    locF));
            card.add(fieldRow("Supplier ID",   supF));
            card.add(Box.createVerticalStrut(4));
            card.add(resultRow);
            card.add(Box.createVerticalStrut(8));
            card.add(btn);
            return card;
        }

        private JPanel buildSupplierCard() {
            JPanel card = buildCard("🏷 Supplier", new Color(0x9B59B6));

            JTextField supplierIdF = LuxuryTheme.textField("SUP-001");
            JTextField nameF       = LuxuryTheme.textField("Supplier Name");
            JSpinner leadDaysSp    = LuxuryTheme.spinner(0, 365, 7);
            JSpinner ratingSp      = new JSpinner(new SpinnerNumberModel(4.0, 0.0, 5.0, 0.1));
            JButton addBtn         = LuxuryTheme.primaryButton("Add / Update Supplier");
            JButton getBtn         = LuxuryTheme.ghostButton("Get Supplier");
            JButton totalBtn       = LuxuryTheme.ghostButton("Supplier Total");
            JLabel infoLabel       = LuxuryTheme.mutedLabel("—");

            addBtn.addActionListener(e -> {
                String supplierId = supplierIdF.getText().trim();
                String name = nameF.getText().trim();
                if (supplierId.isEmpty() || name.isEmpty()) {
                    AppUtils.showError(this, "Supplier ID and Name are required.");
                    return;
                }
                facade.inventoryAddSupplier(
                    supplierId, name,
                    (int) leadDaysSp.getValue(),
                    ((Number) ratingSp.getValue()).doubleValue()
                );
                infoLabel.setText("Saved: " + supplierId);
                log("supplier add/update  id=" + supplierId + " name=" + name + " → OK");
            });

            getBtn.addActionListener(e -> {
                String supplierId = supplierIdF.getText().trim();
                Supplier supplier = facade.inventoryGetSupplier(supplierId);
                if (supplier == null) {
                    infoLabel.setText("Not found");
                    log("supplier get  id=" + supplierId + " → not found");
                    return;
                }
                nameF.setText(supplier.getName());
                leadDaysSp.setValue(supplier.getLeadTimeDays());
                ratingSp.setValue(supplier.getPerformanceRating());
                infoLabel.setText("Found: " + supplier.getName());
                log("supplier get  id=" + supplierId + " → " + supplier.getName());
            });

            totalBtn.addActionListener(e -> {
                String supplierId = supplierIdF.getText().trim();
                int total = facade.inventoryGetTotalStockBySupplier(supplierId);
                infoLabel.setText("Total Stock: " + total);
                log("supplier total  id=" + supplierId + "  totalStock=" + total);
            });

            card.add(fieldRow("Supplier ID *", supplierIdF));
            card.add(fieldRow("Name *", nameF));
            card.add(fieldRow("Lead Days", leadDaysSp));
            card.add(fieldRow("Rating", ratingSp));
            card.add(Box.createVerticalStrut(6));
            card.add(addBtn);
            card.add(Box.createVerticalStrut(4));
            card.add(getBtn);
            card.add(Box.createVerticalStrut(4));
            card.add(totalBtn);
            card.add(Box.createVerticalStrut(6));
            card.add(infoLabel);
            return card;
        }

        private JPanel buildPolicyCard() {
            JPanel card = buildCard("⚙ Issuing Policy", new Color(0xF39C12));
            JComboBox<String> policyCB = LuxuryTheme.comboBox(new String[]{"FIFO", "FEFO"});
            JButton setBtn = LuxuryTheme.primaryButton("Set Policy");
            JButton refreshBtn = LuxuryTheme.ghostButton("Refresh Policy");
            JLabel currentLabel = LuxuryTheme.mutedLabel("Current: " + facade.inventoryGetIssuingPolicy());

            setBtn.addActionListener(e -> {
                String policy = (String) policyCB.getSelectedItem();
                facade.inventorySetIssuingPolicy(policy);
                currentLabel.setText("Current: " + facade.inventoryGetIssuingPolicy());
                log("issuing policy set  " + policy);
            });

            refreshBtn.addActionListener(e -> {
                currentLabel.setText("Current: " + facade.inventoryGetIssuingPolicy());
                log("issuing policy read  " + facade.inventoryGetIssuingPolicy());
            });

            card.add(fieldRow("Policy", policyCB));
            card.add(Box.createVerticalStrut(6));
            card.add(setBtn);
            card.add(Box.createVerticalStrut(4));
            card.add(refreshBtn);
            card.add(Box.createVerticalStrut(6));
            card.add(currentLabel);
            return card;
        }

        private JPanel buildBatchViewCard() {
            JPanel card = buildCard("🧱 Batches", new Color(0x1ABC9C));
            JTextField itemF = LuxuryTheme.textField("SKU or Product ID");
            JTextField locF  = LuxuryTheme.textField("Bin ID or Warehouse ID");
            JTextField supF  = LuxuryTheme.textField("Supplier ID");
            JButton viewBtn  = LuxuryTheme.primaryButton("View Batches");

            viewBtn.addActionListener(e -> {
                String item = itemF.getText().trim();
                String loc = locF.getText().trim();
                String sup = supF.getText().trim();
                if (item.isEmpty() || loc.isEmpty()) {
                    AppUtils.showError(this, "SKU/Product ID and Location are required.");
                    return;
                }
                List<InventoryBatch> batches = facade.inventoryGetBatches(item, loc, sup);
                showBatchDialog("Batches — " + item + " @ " + loc, batches);
                log("batches view  item=" + item + " location=" + loc + " count=" + batches.size());
            });

            card.add(fieldRow("SKU / Product ID *", itemF));
            card.add(fieldRow("Location *", locF));
            card.add(fieldRow("Supplier ID", supF));
            card.add(Box.createVerticalStrut(8));
            card.add(viewBtn);
            return card;
        }

        private JPanel buildTransactionViewCard() {
            JPanel card = buildCard("🧾 Transactions", new Color(0x16A085));
            JTextField supplierF = LuxuryTheme.textField("Supplier ID (optional)");
            JButton viewBtn = LuxuryTheme.primaryButton("View Transactions");

            viewBtn.addActionListener(e -> {
                String supplierId = supplierF.getText().trim();
                List<StockTransaction> all = facade.inventoryGetTransactions();
                List<StockTransaction> filtered = all;
                if (!supplierId.isEmpty()) {
                    filtered = all.stream()
                        .filter(t -> supplierId.equalsIgnoreCase(t.getSupplierId()))
                        .toList();
                }
                showTransactionDialog(
                    supplierId.isEmpty() ? "Inventory Transactions" : "Transactions — " + supplierId,
                    filtered
                );
                log("transactions view  supplier=" + (supplierId.isEmpty() ? "ALL" : supplierId)
                    + " count=" + filtered.size());
            });

            card.add(fieldRow("Supplier Filter", supplierF));
            card.add(Box.createVerticalStrut(8));
            card.add(viewBtn);
            return card;
        }

        private void showBatchDialog(String title, List<InventoryBatch> batches) {
            JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
            d.setSize(760, 420);
            d.setLocationRelativeTo(this);
            d.getContentPane().setBackground(LuxuryTheme.BG_CARD);
            DefaultTableModel model = AppUtils.tableModel(new String[]{
                "Batch ID", "Qty", "Arrival", "Expiry", "Unit Cost", "Serial Count"
            });
            for (InventoryBatch b : batches) {
                model.addRow(new Object[]{
                    b.getBatchId(),
                    b.getQuantity(),
                    b.getArrivalTime(),
                    b.getExpiryTime(),
                    b.getUnitCost(),
                    b.getSerialNumbers() == null ? 0 : b.getSerialNumbers().size()
                });
            }
            JTable table = new JTable(model);
            JScrollPane sp = LuxuryTheme.styledTable(table);
            LuxuryTheme.styleScrollBar(sp.getVerticalScrollBar());
            d.setLayout(new BorderLayout());
            d.add(sp, BorderLayout.CENTER);
            d.setVisible(true);
        }

        private void showTransactionDialog(String title, List<StockTransaction> transactions) {
            JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
            d.setSize(860, 460);
            d.setLocationRelativeTo(this);
            d.getContentPane().setBackground(LuxuryTheme.BG_CARD);
            DefaultTableModel model = AppUtils.tableModel(new String[]{
                "Type", "SKU", "Location", "Supplier", "Qty Change", "Timestamp"
            });
            for (StockTransaction tx : transactions) {
                model.addRow(new Object[]{
                    tx.getType(),
                    tx.getSku(),
                    tx.getLocationId(),
                    tx.getSupplierId(),
                    tx.getQuantityChange(),
                    tx.getTimestamp()
                });
            }
            JTable table = new JTable(model);
            JScrollPane sp = LuxuryTheme.styledTable(table);
            LuxuryTheme.styleScrollBar(sp.getVerticalScrollBar());
            d.setLayout(new BorderLayout());
            d.add(sp, BorderLayout.CENTER);
            d.setVisible(true);
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        private JPanel buildCard(String title, Color accent) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(LuxuryTheme.BG_CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(accent, 1, true),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
            ));
            JLabel lbl = new JLabel(title);
            lbl.setFont(LuxuryTheme.FONT_BODY.deriveFont(java.awt.Font.BOLD));
            lbl.setForeground(accent);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            card.add(lbl);
            return card;
        }

        private JPanel fieldRow(String label, JComponent field) {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setBackground(LuxuryTheme.BG_CARD);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel lbl = new JLabel(label);
            lbl.setFont(LuxuryTheme.FONT_BODY);
            lbl.setForeground(LuxuryTheme.TEXT_MUTED);
            lbl.setPreferredSize(new Dimension(100, 28));
            row.add(lbl,   BorderLayout.WEST);
            row.add(field, BorderLayout.CENTER);
            row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            return row;
        }
    }
}
