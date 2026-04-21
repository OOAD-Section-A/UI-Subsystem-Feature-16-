package scm.ui.orders;

import scm.ui.model.*;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * C-05 — Order Management & Fulfillment UI
 * Tabs: Orders | Order Items | Fulfillment
 * Inputs: orderId, orderStatus, customerName, deliveryAddress, productSKUs, etc.
 * Outputs: new order form, status view, invoice trigger, packing slip print.
 */
public class OrderPanel extends JPanel {

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();

    public OrderPanel() {
        setLayout(new BorderLayout());
        setBackground(LuxuryTheme.BG_PANEL);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LuxuryTheme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 0, 28));
        header.add(LuxuryTheme.sectionTitle("Order Management  &  Fulfillment"), BorderLayout.WEST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(LuxuryTheme.BG_PANEL);
        tabs.setFont(LuxuryTheme.FONT_SUBHEAD);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        tabs.addTab("📋 Orders",       new OrderCrudPanel());
        tabs.addTab("🔖 Order Items",  new OrderItemCrudPanel());
        tabs.addTab("📦 Fulfillment",  new FulfillmentCrudPanel());

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Orders CRUD
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class OrderCrudPanel extends CrudPanel {
        OrderCrudPanel() {
            init("Orders", new String[]{"Order ID","Customer ID","Status","Total","Payment","Channel","Date"});
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (Order o : facade.getAllOrders())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{
                    o.getOrderId(), o.getCustomerId(), o.getOrderStatus(),
                    AppUtils.currency(o.getTotalAmount()), o.getPaymentStatus(),
                    o.getSalesChannel(), o.getOrderDate()}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Create New Order", 520, 440);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("ORD"));
            JTextField custF = LuxuryTheme.textField("CUST-001");
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"PLACED","CONFIRMED","CANCELLED","FULFILLED"});
            JSpinner amtSp = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 9999999.0, 0.01));
            amtSp.setBackground(LuxuryTheme.BG_INPUT); amtSp.setForeground(LuxuryTheme.TEXT_PRIMARY);
            JComboBox<String> payCB  = LuxuryTheme.comboBox(new String[]{"PENDING","PAID","FAILED","REFUNDED"});
            JComboBox<String> chanCB = LuxuryTheme.comboBox(new String[]{"ONLINE","POS","DISTRIBUTOR"});

            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Order Details")
                .addField("Order ID *", idF)
                .addField("Customer ID *", custF)
                .addField("Order Status", stCB)
                .addField("Total Amount (₹)", amtSp)
                .addField("Payment Status", payCB)
                .addField("Sales Channel", chanCB)
                .build();

            addDialogButtons(d, form, () -> {
                Order o = new Order();
                o.setOrderId(idF.getText().trim()); o.setCustomerId(custF.getText().trim());
                o.setOrderStatus((String)stCB.getSelectedItem()); o.setTotalAmount((double)amtSp.getValue());
                o.setPaymentStatus((String)payCB.getSelectedItem()); o.setSalesChannel((String)chanCB.getSelectedItem());
                int r = facade.createOrder(o);
                if (r > 0) { d.dispose(); loadDataAsync(); } else AppUtils.showError(d, "Failed to create order.");
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            Order o = facade.getAllOrders().stream().filter(or -> or.getOrderId().equals(id)).findFirst().orElse(null);
            if (o == null) return;
            JDialog d = createDialog("Edit Order: " + id, 480, 360);
            JComboBox<String> stCB  = LuxuryTheme.comboBox(new String[]{"PLACED","CONFIRMED","CANCELLED","FULFILLED"});
            stCB.setSelectedItem(o.getOrderStatus());
            JComboBox<String> payCB = LuxuryTheme.comboBox(new String[]{"PENDING","PAID","FAILED","REFUNDED"});
            payCB.setSelectedItem(o.getPaymentStatus());
            JSpinner amtSp = new JSpinner(new SpinnerNumberModel(o.getTotalAmount(), 0.0, 9999999.0, 0.01));
            JComboBox<String> chanCB = LuxuryTheme.comboBox(new String[]{"ONLINE","POS","DISTRIBUTOR"});
            chanCB.setSelectedItem(o.getSalesChannel());
            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Edit Order: " + id)
                .addField("Order Status", stCB).addField("Total Amount (₹)", amtSp)
                .addField("Payment Status", payCB).addField("Sales Channel", chanCB).build();
            addDialogButtons(d, form, () -> {
                o.setOrderStatus((String)stCB.getSelectedItem()); o.setTotalAmount((double)amtSp.getValue());
                o.setPaymentStatus((String)payCB.getSelectedItem()); o.setSalesChannel((String)chanCB.getSelectedItem());
                facade.updateOrder(o); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) {
            facade.deleteOrder((String) tableModel.getValueAt(row, 0)); loadDataAsync();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Order Items CRUD
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class OrderItemCrudPanel extends CrudPanel {
        private JTextField orderIdFilter;

        OrderItemCrudPanel() {
            setLayout(new BorderLayout());
            setBackground(LuxuryTheme.BG_PANEL);
            JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            filterPanel.setBackground(LuxuryTheme.BG_PANEL);
            filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 28));
            filterPanel.add(LuxuryTheme.mutedLabel("Filter by Order ID:"));
            orderIdFilter = LuxuryTheme.textField("ORD-001");
            orderIdFilter.setPreferredSize(new Dimension(180, 34));
            JButton filterBtn = LuxuryTheme.ghostButton("Filter");
            filterBtn.addActionListener(e -> loadDataAsync());
            filterPanel.add(orderIdFilter);
            filterPanel.add(filterBtn);
            init("Order Items", new String[]{"Item ID","Order ID","Product ID","Qty","Unit Price","Line Total"});
            add(filterPanel, BorderLayout.NORTH);
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            String filter = orderIdFilter != null ? orderIdFilter.getText().trim() : "";
            List<OrderItem> items;
            if (!filter.isEmpty() && !filter.equals("ORD-001"))
                items = facade.getOrderItems(filter);
            else {
                items = facade.getAllOrders().stream()
                    .limit(20)
                    .flatMap(o -> facade.getOrderItems(o.getOrderId()).stream())
                    .collect(java.util.stream.Collectors.toList());
            }
            for (OrderItem i : items)
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{
                    i.getOrderItemId(), i.getOrderId(), i.getProductId(),
                    i.getOrderedQuantity(), AppUtils.currency(i.getUnitPrice()), AppUtils.currency(i.getLineTotal())}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Add Order Item", 480, 360);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("OI"));
            JTextField ordF  = LuxuryTheme.textField("ORD-001");
            JTextField pidF  = LuxuryTheme.textField("PROD-001");
            JSpinner qtySp   = LuxuryTheme.spinner(1, 9999, 1);
            JSpinner priceSp = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 999999.0, 0.01));
            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Order Item Details")
                .addField("Item ID *", idF).addField("Order ID *", ordF).addField("Product ID *", pidF)
                .addField("Quantity *", qtySp).addField("Unit Price (₹) *", priceSp).build();
            addDialogButtons(d, form, () -> {
                OrderItem i = new OrderItem();
                i.setOrderItemId(idF.getText().trim()); i.setOrderId(ordF.getText().trim()); i.setProductId(pidF.getText().trim());
                i.setOrderedQuantity((int)qtySp.getValue()); i.setUnitPrice((double)priceSp.getValue());
                i.setLineTotal(i.getOrderedQuantity() * i.getUnitPrice());
                facade.createOrderItem(i); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            AppUtils.showError(this, "Select item and use the Update dialog.");
        }

        @Override protected void deleteRow(int row) {
            facade.deleteOrderItem((String) tableModel.getValueAt(row, 0)); loadDataAsync();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Fulfillment CRUD
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class FulfillmentCrudPanel extends CrudPanel {
        FulfillmentCrudPanel() {
            init("Fulfillment Orders", new String[]{"Fulfillment ID","Order ID","Status","Warehouse","Priority","Created At"});
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (FulfillmentOrder f : facade.getAllFulfillments())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{
                    f.getFulfillmentId(), f.getOrderId(), f.getFulfillmentStatus(),
                    f.getAssignedWarehouse(), f.getPriorityLevel(), f.getCreatedAt()}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Create Fulfillment", 480, 340);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("FUL"));
            JTextField ordF  = LuxuryTheme.textField("ORD-001");
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"PENDING","PROCESSING","PACKED","DISPATCHED","DELIVERED"});
            JTextField whF   = LuxuryTheme.textField("WH-001");
            JComboBox<String> priCB = LuxuryTheme.comboBox(new String[]{"HIGH","MEDIUM","LOW"});
            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Fulfillment Details")
                .addField("Fulfillment ID *", idF).addField("Order ID *", ordF).addField("Status", stCB)
                .addField("Warehouse ID *", whF).addField("Priority", priCB).build();
            addDialogButtons(d, form, () -> {
                FulfillmentOrder f = new FulfillmentOrder();
                f.setFulfillmentId(idF.getText().trim()); f.setOrderId(ordF.getText().trim());
                f.setFulfillmentStatus((String)stCB.getSelectedItem()); f.setAssignedWarehouse(whF.getText().trim());
                f.setPriorityLevel((String)priCB.getSelectedItem());
                facade.createFulfillment(f); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            FulfillmentOrder f = facade.getAllFulfillments().stream().filter(ff -> ff.getFulfillmentId().equals(id)).findFirst().orElse(null);
            if (f == null) return;
            JDialog d = createDialog("Edit Fulfillment: " + id, 440, 280);
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"PENDING","PROCESSING","PACKED","DISPATCHED","DELIVERED"});
            stCB.setSelectedItem(f.getFulfillmentStatus());
            JTextField whF = LuxuryTheme.textField(f.getAssignedWarehouse());
            JComboBox<String> priCB = LuxuryTheme.comboBox(new String[]{"HIGH","MEDIUM","LOW"});
            priCB.setSelectedItem(f.getPriorityLevel());
            JPanel form = new CrudPanel.FormBuilder().addField("Status", stCB).addField("Warehouse", whF).addField("Priority", priCB).build();
            addDialogButtons(d, form, () -> {
                f.setFulfillmentStatus((String)stCB.getSelectedItem()); f.setAssignedWarehouse(whF.getText().trim()); f.setPriorityLevel((String)priCB.getSelectedItem());
                facade.updateFulfillment(f); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deleteFulfillment((String)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }
}
