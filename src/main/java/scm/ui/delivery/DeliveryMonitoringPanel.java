package scm.ui.delivery;

import com.ramennoodles.delivery.enums.OrderStatus;
import com.ramennoodles.delivery.enums.RiderStatus;
import com.ramennoodles.delivery.facade.DeliveryMonitoringFacadeDB;
import com.ramennoodles.delivery.model.Coordinate;
import com.ramennoodles.delivery.model.Customer;
import com.ramennoodles.delivery.model.Device;
import com.ramennoodles.delivery.model.ETARecord;
import com.ramennoodles.delivery.model.GPSPing;
import com.ramennoodles.delivery.model.Order;
import com.ramennoodles.delivery.model.PODRecord;
import com.ramennoodles.delivery.model.Rider;
import com.ramennoodles.delivery.observer.DeliveryEventListener;
import com.ramennoodles.delivery.observer.DeliveryEventType;
import scm.ui.patterns.EventBus;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.LuxuryTheme;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * C-11 — Real-Time Delivery Monitoring Panel
 *
 * Full integration of the Ramen Noodles (#9) Real-Time Delivery Monitoring
 * System into the SCM Nexus UI Subsystem.
 *
 * All delivery operations are routed through SupplyChainFacade.getDeliverySystem()
 * so the UI never couples directly to DeliveryMonitoringFacadeDB — consistent with
 * the existing architecture pattern.
 *
 * Tabs:
 *   1. 📍 Live GPS Tracking  — active orders with real-time GPS, auto-refresh
 *   2. 📦 Order Management   — create delivery, assign rider, update status
 *   3. 🏁 Fleet & Register   — register customer / rider / device, view riders
 *   4. ✅ POD & Completion   — complete delivery, display POD
 *   5. 🔧 ETA & Controls     — query ETA, simulate GPS pings, status history
 *
 * Events bridged to UI EventBus:
 *   DeliveryEventType.ORDER_DELIVERED   → EventBus.Event.DELIVERY_STATUS_CHANGED
 *   DeliveryEventType.STATUS_CHANGED    → EventBus.Event.DELIVERY_STATUS_CHANGED
 *   DeliveryEventType.LOCATION_UPDATED  → EventBus.Event.DELIVERY_GPS_UPDATED
 *   DeliveryEventType.POD_SUBMITTED     → EventBus.Event.DELIVERY_POD_SUBMITTED
 *   DeliveryEventType.ORDER_FAILED      → EventBus.Event.DELIVERY_STATUS_CHANGED
 *   DeliveryEventType.ETA_UPDATED       → EventBus.Event.DELIVERY_ETA_UPDATED
 *   DeliveryEventType.GEOFENCE_ENTRY    → EventBus.Event.DELIVERY_STATUS_CHANGED
 *   DeliveryEventType.GEOFENCE_EXIT     → EventBus.Event.DELIVERY_STATUS_CHANGED
 */
public class DeliveryMonitoringPanel extends JPanel {

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();

    // ── Sub-panel references (for event-driven refresh) ──────────────────────
    private LiveTrackingTab liveTrackingTab;

    // ── Auto-refresh scheduler ────────────────────────────────────────────────
    private ScheduledExecutorService autoRefreshScheduler;

    public DeliveryMonitoringPanel() {
        setLayout(new BorderLayout());
        setBackground(LuxuryTheme.BG_PANEL);
        buildUI();
        subscribeDeliveryEvents();
        startAutoRefresh();
    }

    // ── Called when this panel is hidden (e.g. navigation away) ─────────────
    public void shutdown() {
        if (autoRefreshScheduler != null && !autoRefreshScheduler.isShutdown()) {
            autoRefreshScheduler.shutdownNow();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI
    // ─────────────────────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LuxuryTheme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 0, 28));
        header.add(LuxuryTheme.sectionTitle("Real-Time Delivery Monitoring"), BorderLayout.WEST);

        // Status pill
        JLabel statusPill = new JLabel("● LIVE");
        statusPill.setFont(LuxuryTheme.FONT_SMALL);
        statusPill.setForeground(new Color(80, 200, 120));
        statusPill.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        header.add(statusPill, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(LuxuryTheme.BG_PANEL);
        tabs.setFont(LuxuryTheme.FONT_SUBHEAD);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        liveTrackingTab = new LiveTrackingTab();

        tabs.addTab("📍 Live GPS Tracking",  liveTrackingTab);
        tabs.addTab("📦 Order Management",   new OrderManagementTab());
        tabs.addTab("🏁 Fleet & Register",   new FleetRegisterTab());
        tabs.addTab("✅ POD & Completion",   new PODTab());
        tabs.addTab("🔧 ETA & Controls",     new ETAControlsTab());

        add(header,  BorderLayout.NORTH);
        add(tabs,    BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVENT BRIDGING — Delivery System → UI EventBus
    // ─────────────────────────────────────────────────────────────────────────

    private void subscribeDeliveryEvents() {
        DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
        if (ds == null) return;

        EventBus bus = EventBus.getInstance();

        // Bridge: any status change → DELIVERY_STATUS_CHANGED
        DeliveryEventListener statusListener = (type, data) ->
            SwingUtilities.invokeLater(() -> {
                bus.publish(EventBus.Event.DELIVERY_STATUS_CHANGED, data);
                if (liveTrackingTab != null) liveTrackingTab.refreshTable();
                appendEventLog(type.name(), data);
            });

        // Bridge: GPS update → DELIVERY_GPS_UPDATED
        DeliveryEventListener gpsListener = (type, data) ->
            SwingUtilities.invokeLater(() -> {
                bus.publish(EventBus.Event.DELIVERY_GPS_UPDATED, data);
                if (liveTrackingTab != null) liveTrackingTab.refreshTable();
            });

        // Bridge: POD submitted → DELIVERY_POD_SUBMITTED
        DeliveryEventListener podListener = (type, data) ->
            SwingUtilities.invokeLater(() -> {
                bus.publish(EventBus.Event.DELIVERY_POD_SUBMITTED, data);
                appendEventLog(type.name(), data);
            });

        // Bridge: ETA updated → DELIVERY_ETA_UPDATED
        DeliveryEventListener etaListener = (type, data) ->
            SwingUtilities.invokeLater(() -> {
                bus.publish(EventBus.Event.DELIVERY_ETA_UPDATED, data);
                if (liveTrackingTab != null) liveTrackingTab.refreshTable();
            });

        ds.subscribeToEvents(DeliveryEventType.ORDER_CREATED,    statusListener);
        ds.subscribeToEvents(DeliveryEventType.RIDER_ASSIGNED,   statusListener);
        ds.subscribeToEvents(DeliveryEventType.STATUS_CHANGED,   statusListener);
        ds.subscribeToEvents(DeliveryEventType.ORDER_DELIVERED,  statusListener);
        ds.subscribeToEvents(DeliveryEventType.ORDER_FAILED,     statusListener);
        ds.subscribeToEvents(DeliveryEventType.GEOFENCE_ENTRY,   statusListener);
        ds.subscribeToEvents(DeliveryEventType.GEOFENCE_EXIT,    statusListener);
        ds.subscribeToEvents(DeliveryEventType.LOCATION_UPDATED, gpsListener);
        ds.subscribeToEvents(DeliveryEventType.POD_SUBMITTED,    podListener);
        ds.subscribeToEvents(DeliveryEventType.ETA_UPDATED,      etaListener);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTO-REFRESH (every 30 s) — refreshes live tracking table
    // ─────────────────────────────────────────────────────────────────────────

    private void startAutoRefresh() {
        autoRefreshScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DeliveryAutoRefresh");
            t.setDaemon(true);
            return t;
        });
        autoRefreshScheduler.scheduleAtFixedRate(() ->
            SwingUtilities.invokeLater(() -> {
                if (liveTrackingTab != null) liveTrackingTab.refreshTable();
            }),
            30, 30, TimeUnit.SECONDS);
    }

    // ── Shared event log (appended to by all tabs for diagnostic display) ────
    private final DefaultTableModel eventLogModel = new DefaultTableModel(
        new String[]{"Time", "Event", "Order ID", "Rider ID", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private void appendEventLog(String eventName, Map<String, Object> data) {
        String time     = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        String orderId  = data != null && data.get("orderId")  != null ? data.get("orderId").toString()  : "—";
        String riderId  = data != null && data.get("riderId")  != null ? data.get("riderId").toString()  : "—";
        String status   = data != null && data.get("newStatus") != null ? data.get("newStatus").toString() : "—";
        SwingUtilities.invokeLater(() -> eventLogModel.insertRow(0, new Object[]{time, eventName, orderId, riderId, status}));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 1 — LIVE GPS TRACKING
    // ─────────────────────────────────────────────────────────────────────────

    class LiveTrackingTab extends JPanel {

        private final DefaultTableModel tableModel;
        private final JTable            table;
        private final JLabel            infoLabel;
        private boolean                 loading;

        LiveTrackingTab() {
            setBackground(LuxuryTheme.BG_PANEL);
            setLayout(new BorderLayout(0, 10));
            setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

            // ── Top bar ───────────────────────────────────────────────────
            JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            topBar.setBackground(LuxuryTheme.BG_PANEL);
            topBar.add(LuxuryTheme.subHeading("Active Delivery Orders — Live GPS Feed"));

            JButton refreshBtn = LuxuryTheme.primaryButton("↻  Refresh Now");
            refreshBtn.addActionListener(e -> refreshTable());
            topBar.add(refreshBtn);

            infoLabel = new JLabel("Press Refresh to load live data");
            infoLabel.setForeground(LuxuryTheme.TEXT_SECOND);
            infoLabel.setFont(LuxuryTheme.FONT_SMALL);
            topBar.add(infoLabel);

            // ── Table ─────────────────────────────────────────────────────
            tableModel = new DefaultTableModel(
                new String[]{"Order ID", "Customer", "Rider", "Status",
                             "Latitude", "Longitude", "ETA (min)", "Pickup Address", "Dropoff Address"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            table = new JTable(tableModel);
            LuxuryTheme.styledTable(table);
            table.setFillsViewportHeight(true);

            // ── Event log section ─────────────────────────────────────────
            JLabel evtTitle = LuxuryTheme.subHeading("Real-Time Event Log");
            JTable evtTable = new JTable(eventLogModel);
            LuxuryTheme.styledTable(evtTable);
            evtTable.setFillsViewportHeight(true);
            JScrollPane evtScroll = new JScrollPane(evtTable);
            evtScroll.setPreferredSize(new Dimension(0, 160));
            evtScroll.setBorder(new LineBorder(LuxuryTheme.BORDER_SUBTLE));
            evtScroll.setBackground(LuxuryTheme.BG_PANEL);
            evtScroll.getViewport().setBackground(LuxuryTheme.BG_PANEL);

            JPanel evtPanel = new JPanel(new BorderLayout(0, 6));
            evtPanel.setBackground(LuxuryTheme.BG_PANEL);
            evtPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
            evtPanel.add(evtTitle, BorderLayout.NORTH);
            evtPanel.add(evtScroll, BorderLayout.CENTER);

            JScrollPane tableScroll = new JScrollPane(table);
            tableScroll.setBorder(new LineBorder(LuxuryTheme.BORDER_SUBTLE));
            tableScroll.setBackground(LuxuryTheme.BG_PANEL);
            tableScroll.getViewport().setBackground(LuxuryTheme.BG_PANEL);

            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, evtPanel);
            split.setResizeWeight(0.65);
            split.setDividerSize(4);
            split.setBackground(LuxuryTheme.BG_PANEL);

            add(topBar, BorderLayout.NORTH);
            add(split,  BorderLayout.CENTER);

            refreshTable();
        }

        void refreshTable() {
            if (loading) return;
            loading = true;
            infoLabel.setText("Refreshing...");

            SwingWorker<java.util.List<Object[]>, Void> worker = new SwingWorker<>() {
                @Override protected java.util.List<Object[]> doInBackground() {
                    return facade.getDeliveryLiveRows();
                }
                @Override protected void done() {
                    loading = false;
                    tableModel.setRowCount(0);
                    try {
                        for (Object[] row : get()) {
                            tableModel.addRow(row);
                        }
                    } catch (Exception ex) {
                        infoLabel.setText("Refresh failed: " + ex.getMessage());
                        return;
                    }
                    infoLabel.setText("Last refreshed: " +
                        new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
                }
            };
            worker.execute();
        }

        /** Called by event callbacks to push a new GPS row into the table. */
        void pushGPSRow(String orderId, String riderId, double lat, double lng, String status) {
            SwingUtilities.invokeLater(() -> {
                // Update existing row if orderId already present
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (orderId.equals(tableModel.getValueAt(i, 0))) {
                        tableModel.setValueAt(riderId, i, 2);
                        tableModel.setValueAt(status,  i, 3);
                        tableModel.setValueAt(String.format("%.5f", lat), i, 4);
                        tableModel.setValueAt(String.format("%.5f", lng), i, 5);
                        return;
                    }
                }
                tableModel.addRow(new Object[]{
                    orderId, "—", riderId, status,
                    String.format("%.5f", lat), String.format("%.5f", lng), "—", "—", "—"
                });
            });
        }

        /** Called when an order is created to add it to the live table. */
        void addOrderRow(Order order) {
            if (order == null) return;
            SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{
                order.getOrderId(),
                order.getCustomerId(),
                order.getRiderId() != null ? order.getRiderId() : "Unassigned",
                order.getStatus() != null ? order.getStatus().name() : "CREATED",
                "—", "—", "—",
                order.getPickupAddress(),
                order.getDropoffAddress()
            }));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 2 — ORDER MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    class OrderManagementTab extends JPanel {

        private final JTextArea logArea;

        OrderManagementTab() {
            setBackground(LuxuryTheme.BG_PANEL);
            setLayout(new BorderLayout(0, 0));
            setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

            JLabel title = LuxuryTheme.subHeading("Order Lifecycle Management");

            // ── Cards ─────────────────────────────────────────────────────
            JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
            cards.setBackground(LuxuryTheme.BG_PANEL);
            cards.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

            logArea = new JTextArea(8, 0);
            logArea.setEditable(false);
            logArea.setBackground(LuxuryTheme.BG_CARD);
            logArea.setForeground(LuxuryTheme.TEXT_PRIMARY);
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            logArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            JScrollPane logScroll = new JScrollPane(logArea);
            logScroll.setBorder(new LineBorder(LuxuryTheme.BORDER_SUBTLE));

            // Card 1 — Create Delivery
            cards.add(buildCreateOrderCard());
            // Card 2 — Assign Rider
            cards.add(buildAssignRiderCard());
            // Card 3 — Update Status
            cards.add(buildUpdateStatusCard());

            add(title,     BorderLayout.NORTH);
            add(cards,     BorderLayout.CENTER);

            JPanel logPanel = new JPanel(new BorderLayout(0, 6));
            logPanel.setBackground(LuxuryTheme.BG_PANEL);
            logPanel.add(LuxuryTheme.subHeading("Operation Log"), BorderLayout.NORTH);
            logPanel.add(logScroll, BorderLayout.CENTER);
            add(logPanel,  BorderLayout.SOUTH);
        }

        private void log(String msg) {
            String ts = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            logArea.append("[" + ts + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }

        private JPanel buildCreateOrderCard() {
            JPanel card = makeCard("📦 Create Delivery Order");

            JTextField custIdFld    = field("Customer ID");
            JTextField pickupAddrFld= field("Pickup Address");
            JTextField dropoffFld   = field("Dropoff Address");
            JTextField pickLatFld   = field("Pickup Lat  (e.g. 12.9352)");
            JTextField pickLngFld   = field("Pickup Lng  (e.g. 77.6245)");
            JTextField dropLatFld   = field("Dropoff Lat (e.g. 12.9716)");
            JTextField dropLngFld   = field("Dropoff Lng (e.g. 77.5946)");

            JButton createBtn = LuxuryTheme.primaryButton("Create Order");
            createBtn.addActionListener(e -> {
                DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
                if (ds == null) { log("ERROR: Delivery system not initialized."); return; }
                try {
                    String customerId    = custIdFld.getText().trim();
                    String pickupAddress = pickupAddrFld.getText().trim();
                    String dropoffAddress= dropoffFld.getText().trim();
                    double pickLat = Double.parseDouble(pickLatFld.getText().trim());
                    double pickLng = Double.parseDouble(pickLngFld.getText().trim());
                    double dropLat = Double.parseDouble(dropLatFld.getText().trim());
                    double dropLng = Double.parseDouble(dropLngFld.getText().trim());

                    Order order = ds.createAndInitializeDelivery(
                        customerId,
                        pickupAddress,
                        dropoffAddress,
                        new Coordinate(pickLat, pickLng),
                        new Coordinate(dropLat, dropLng)
                    );
                    if (order != null) {
                        boolean geoSynced = facade.syncOrderGeoSnapshot(
                            order.getOrderId(),
                            customerId,
                            pickupAddress,
                            dropoffAddress,
                            pickLat, pickLng,
                            dropLat, dropLng
                        );
                        log("✅ Order created: " + order.getOrderId());
                        if (geoSynced) {
                            if (liveTrackingTab != null) liveTrackingTab.refreshTable();
                        } else {
                            log("⚠ Order created, but geo sync to orders table failed.");
                            if (liveTrackingTab != null) liveTrackingTab.addOrderRow(order);
                        }
                    } else {
                        String fallbackOrderId = createOrderViaDbFallback(
                            customerId, pickupAddress, dropoffAddress, pickLat, pickLng, dropLat, dropLng);
                        if (fallbackOrderId != null) {
                            log("✅ Order created via DB fallback: " + fallbackOrderId);
                            if (liveTrackingTab != null) liveTrackingTab.refreshTable();
                        } else {
                            log("❌ Order creation failed in API and DB fallback.");
                        }
                    }
                } catch (NumberFormatException ex) {
                    log("❌ Invalid lat/lng — must be decimal numbers.");
                } catch (Exception ex) {
                    log("❌ Error: " + ex.getMessage());
                }
            });

            for (JTextField f : new JTextField[]{custIdFld, pickupAddrFld, dropoffFld,
                                                  pickLatFld, pickLngFld, dropLatFld, dropLngFld}) {
                card.add(f);
            }
            card.add(createBtn);
            return card;
        }

        private JPanel buildAssignRiderCard() {
            JPanel card = makeCard("🛵 Assign Rider to Order");

            JTextField orderIdFld = field("Order ID");
            JTextField riderIdFld = field("Rider ID");

            JButton assignBtn = LuxuryTheme.primaryButton("Assign Rider");
            assignBtn.addActionListener(e -> {
                int updated = facade.assignDeliveryRiderForOrder(
                        orderIdFld.getText().trim(),
                        riderIdFld.getText().trim()
                );
                if (updated > 0) {
                    log("✅ Rider " + riderIdFld.getText().trim() +
                            " assigned to order " + orderIdFld.getText().trim());
                    if (liveTrackingTab != null) liveTrackingTab.refreshTable();
                } else {
                    log("❌ Order not found in DB: " + orderIdFld.getText().trim());
                }
            });

            // Query order status
            JTextField queryOrderFld = field("Query Order ID");
            JButton queryBtn = LuxuryTheme.ghostButton("Get Status");
            queryBtn.addActionListener(e -> {
                String status = facade.getDeliveryStatusForOrder(queryOrderFld.getText().trim());
                log("📋 Order " + queryOrderFld.getText().trim() +
                        " status: " + (status != null ? status : "NOT FOUND"));
            });

            // Tracking URL
            JTextField trackOrderFld = field("Tracking URL for Order ID");
            JButton trackBtn = LuxuryTheme.ghostButton("Get Tracking URL");
            trackBtn.addActionListener(e -> {
                String url = facade.getDeliveryTrackingUrl(trackOrderFld.getText().trim());
                log("🔗 Tracking URL: " + (url != null ? url : "NOT FOUND"));
            });

            for (JComponent c : new JComponent[]{orderIdFld, riderIdFld, assignBtn,
                                                   queryOrderFld, queryBtn,
                                                   trackOrderFld, trackBtn}) {
                card.add(c);
            }
            return card;
        }

        private JPanel buildUpdateStatusCard() {
            JPanel card = makeCard("🔄 Update Order Status");

            JTextField orderIdFld = field("Order ID");
            JComboBox<String> statusBox = new JComboBox<>(new String[]{
                "CREATED", "ASSIGNED", "PICKED_UP", "IN_TRANSIT",
                "ARRIVING", "DELIVERED", "FAILED", "CANCELLED", "RETURNED"
            });
            statusBox.setBackground(LuxuryTheme.BG_CARD);
            statusBox.setForeground(LuxuryTheme.TEXT_PRIMARY);
            statusBox.setFont(LuxuryTheme.FONT_BODY);
            statusBox.setMaximumRowCount(8);
            statusBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            statusBox.setPreferredSize(new Dimension(320, 32));
            statusBox.setAlignmentX(Component.LEFT_ALIGNMENT);

            JTextField changedByFld = field("Changed By (user/system)");

            JButton updateBtn = LuxuryTheme.primaryButton("Update Status");
            updateBtn.addActionListener(e -> {
                String newStatus = (String) statusBox.getSelectedItem();
                int updated = facade.updateDeliveryStatusForOrder(
                        orderIdFld.getText().trim(),
                        newStatus,
                        changedByFld.getText().trim()
                );
                if (updated > 0) {
                    log("✅ Order " + orderIdFld.getText().trim() +
                            " status updated to " + newStatus);
                    if (liveTrackingTab != null) liveTrackingTab.refreshTable();
                } else {
                    log("❌ Order not found in DB: " + orderIdFld.getText().trim());
                }
            });

            for (JComponent c : new JComponent[]{orderIdFld, statusBox, changedByFld, updateBtn}) {
                card.add(c);
            }
            return card;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 3 — FLEET & REGISTRATION
    // ─────────────────────────────────────────────────────────────────────────

    class FleetRegisterTab extends JPanel {

        private final JTextArea logArea;

        FleetRegisterTab() {
            setBackground(LuxuryTheme.BG_PANEL);
            setLayout(new BorderLayout(0, 0));
            setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

            JLabel title = LuxuryTheme.subHeading("Fleet Management & Entity Registration");

            JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
            cards.setBackground(LuxuryTheme.BG_PANEL);
            cards.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

            logArea = new JTextArea(8, 0);
            logArea.setEditable(false);
            logArea.setBackground(LuxuryTheme.BG_CARD);
            logArea.setForeground(LuxuryTheme.TEXT_PRIMARY);
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            logArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            JScrollPane logScroll = new JScrollPane(logArea);
            logScroll.setBorder(new LineBorder(LuxuryTheme.BORDER_SUBTLE));

            cards.add(buildRegisterCustomerCard());
            cards.add(buildRegisterRiderCard());
            cards.add(buildRegisterDeviceCard());

            add(title, BorderLayout.NORTH);
            add(cards, BorderLayout.CENTER);

            JPanel logPanel = new JPanel(new BorderLayout(0, 6));
            logPanel.setBackground(LuxuryTheme.BG_PANEL);
            logPanel.add(LuxuryTheme.subHeading("Registration Log"), BorderLayout.NORTH);
            logPanel.add(logScroll, BorderLayout.CENTER);
            add(logPanel, BorderLayout.SOUTH);
        }

        private void log(String msg) {
            String ts = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            logArea.append("[" + ts + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }

        private JPanel buildRegisterCustomerCard() {
            JPanel card = makeCard("👤 Register Customer");

            JTextField nameFld  = field("Full Name");
            JTextField emailFld = field("Email Address");
            JTextField phoneFld = field("Phone Number");

            JButton regBtn = LuxuryTheme.primaryButton("Register Customer");
            regBtn.addActionListener(e -> {
                DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
                if (ds == null) { log("ERROR: Delivery system not initialized."); return; }
                try {
                    Customer c = ds.registerCustomer(
                        nameFld.getText().trim(),
                        emailFld.getText().trim(),
                        phoneFld.getText().trim()
                    );
                    if (c != null) log("✅ Customer registered: ID=" + c.getCustomerId() + " Name=" + c.getName());
                    else           log("❌ Customer registration failed.");
                } catch (Exception ex) {
                    log("❌ Error: " + ex.getMessage());
                }
            });

            for (JComponent c : new JComponent[]{nameFld, emailFld, phoneFld, regBtn}) {
                card.add(c);
            }
            return card;
        }

        private JPanel buildRegisterRiderCard() {
            JPanel card = makeCard("🛵 Register Rider");

            JTextField nameFld    = field("Rider Name");
            JTextField phoneFld   = field("Phone Number");
            JTextField vehicleFld = field("Vehicle Type (BIKE/CAR/VAN)");

            JButton regBtn = LuxuryTheme.primaryButton("Register Rider");
            regBtn.addActionListener(e -> {
                DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
                if (ds == null) { log("ERROR: Delivery system not initialized."); return; }
                try {
                    Rider r = ds.registerRider(
                        nameFld.getText().trim(),
                        phoneFld.getText().trim(),
                        vehicleFld.getText().trim()
                    );
                    if (r != null) log("✅ Rider registered: ID=" + r.getRiderId() + " Name=" + r.getName());
                    else           log("❌ Rider registration failed.");
                } catch (Exception ex) {
                    log("❌ Error: " + ex.getMessage());
                }
            });

            // Query rider position
            JTextField riderPosFld = field("Rider ID (for GPS position)");
            JButton posBtn = LuxuryTheme.ghostButton("Get Position");
            posBtn.addActionListener(e -> {
                DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
                if (ds == null) return;
                String riderId = riderPosFld.getText().trim();
                try {
                    GPSPing ping = ds.getRiderPosition(riderId);
                    if (ping != null) {
                        log("📍 Rider " + riderId +
                            " at lat=" + ping.getLatitude() + " lng=" + ping.getLongitude());
                    } else {
                        double[] fallback = facade.getLatestRiderCoordinates(riderId);
                        if (fallback != null) {
                            log("📍 Rider " + riderId +
                                " at lat=" + String.format("%.5f", fallback[0]) +
                                " lng=" + String.format("%.5f", fallback[1]) + " (DB)");
                        } else {
                            log("ℹ No GPS data for rider " + riderId);
                        }
                    }
                } catch (Exception ex) {
                    log("❌ Error: " + ex.getMessage());
                }
            });

            for (JComponent c : new JComponent[]{nameFld, phoneFld, vehicleFld, regBtn, riderPosFld, posBtn}) {
                card.add(c);
            }
            return card;
        }

        private JPanel buildRegisterDeviceCard() {
            JPanel card = makeCard("📡 Register GPS Device");

            JLabel note = new JLabel("<html><i>Enter the Rider ID whose device to register.<br>"
                + "Device is linked to the rider automatically.</i></html>");
            note.setForeground(LuxuryTheme.TEXT_SECOND);
            note.setFont(LuxuryTheme.FONT_SMALL);

            JTextField riderIdFld = field("Rider ID");

            JButton regBtn = LuxuryTheme.primaryButton("Register Device");
            regBtn.addActionListener(e -> {
                DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
                if (ds == null) { log("ERROR: Delivery system not initialized."); return; }
                try {
                    // Must obtain the Rider object first via getRiderPosition (indirect).
                    // Since DeliveryMonitoringFacadeDB does not expose getRider(id) directly,
                    // we call registerRider with placeholder data to get the Rider, then
                    // in a real integration the rider object would come from the Fleet system.
                    // For now: create a temporary Rider shell just to register the device.
                    Rider tempRider = new Rider();
                    tempRider.setRiderId(riderIdFld.getText().trim());
                    tempRider.setName("Device-Only");
                    tempRider.setPhone("");
                    tempRider.setVehicleType("BIKE");
                    Device d = ds.registerDeviceForRider(tempRider);
                    if (d != null) log("✅ Device registered: ID=" + d.getDeviceId() +
                                       " for Rider=" + riderIdFld.getText().trim());
                    else           log("❌ Device registration failed.");
                } catch (Exception ex) {
                    log("❌ Error: " + ex.getMessage());
                }
            });

            for (JComponent c : new JComponent[]{note, riderIdFld, regBtn}) {
                card.add(c);
            }
            return card;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 4 — POD & COMPLETION
    // ─────────────────────────────────────────────────────────────────────────

    class PODTab extends JPanel {

        private final JTextArea logArea;
        private final DefaultTableModel podTableModel;

        PODTab() {
            setBackground(LuxuryTheme.BG_PANEL);
            setLayout(new BorderLayout(0, 10));
            setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

            JLabel title = LuxuryTheme.subHeading("Proof of Delivery (ePOD) & Completion");

            // POD table
            podTableModel = new DefaultTableModel(
                new String[]{"Order ID", "Rider ID", "Signature", "Photo", "Notes", "Submitted At"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable podTable = new JTable(podTableModel);
            LuxuryTheme.styledTable(podTable);
            podTable.setFillsViewportHeight(true);
            JScrollPane podScroll = new JScrollPane(podTable);
            podScroll.setBorder(new LineBorder(LuxuryTheme.BORDER_SUBTLE));
            podScroll.setBackground(LuxuryTheme.BG_PANEL);
            podScroll.getViewport().setBackground(LuxuryTheme.BG_PANEL);

            // Form
            JPanel formPanel = new JPanel(new GridLayout(1, 2, 16, 0));
            formPanel.setBackground(LuxuryTheme.BG_PANEL);
            formPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

            formPanel.add(buildCompleteDeliveryCard());
            formPanel.add(buildQueryPODCard());

            logArea = new JTextArea(3, 0);
            logArea.setEditable(false);
            logArea.setBackground(LuxuryTheme.BG_CARD);
            logArea.setForeground(LuxuryTheme.TEXT_PRIMARY);
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            logArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            JScrollPane logScroll = new JScrollPane(logArea);
            logScroll.setBorder(new LineBorder(LuxuryTheme.BORDER_SUBTLE));
            logScroll.setPreferredSize(new Dimension(0, 92));

            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, podScroll, logScroll);
            split.setResizeWeight(0.9);
            split.setDividerSize(4);
            split.setBackground(LuxuryTheme.BG_PANEL);
            split.setBorder(BorderFactory.createEmptyBorder());

            JPanel topSection = new JPanel(new BorderLayout(0, 0));
            topSection.setBackground(LuxuryTheme.BG_PANEL);
            topSection.add(title, BorderLayout.NORTH);
            topSection.add(formPanel, BorderLayout.CENTER);

            add(topSection, BorderLayout.NORTH);
            add(split,      BorderLayout.CENTER);

            refreshPodTable();
        }

        private void log(String msg) {
            String ts = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            logArea.append("[" + ts + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }

        private void refreshPodTable() {
            podTableModel.setRowCount(0);
            for (Object[] row : facade.getDeliveryPodRows()) {
                podTableModel.addRow(row);
            }
        }

        private JPanel buildCompleteDeliveryCard() {
            JPanel card = makeCard("✅ Complete Delivery (Submit POD)");

            JTextField orderIdFld  = field("Order ID");
            JTextField sigFileFld  = field("Signature File Path");
            JTextField photoFileFld= field("Photo File Path");
            JTextField notesFld    = field("Delivery Notes");

            JButton completeBtn = LuxuryTheme.primaryButton("Complete Delivery");
            completeBtn.addActionListener(e -> {
                DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
                String orderId = orderIdFld.getText().trim();
                String sigPath = sigFileFld.getText().trim();
                String photoPath = photoFileFld.getText().trim();
                String notes = notesFld.getText().trim();
                try {
                    PODRecord pod = null;
                    if (ds != null) {
                        pod = ds.completeDelivery(orderId, sigPath, photoPath, notes);
                    }

                    boolean completed = (pod != null) || facade.completeDeliveryWithPodFallback(orderId, sigPath, photoPath, notes);
                    if (pod != null) {
                        orderId = pod.getOrderId();
                    }

                    if (completed) {
                        log("✅ Delivery completed for order " + orderId);
                        refreshPodTable();
                        facade.addSystemNotification("DELIVERY", "Order " + orderId + " delivered successfully — ePOD captured.");
                    } else {
                        log("❌ Delivery completion failed.");
                    }
                } catch (Exception ex) {
                    boolean completed = facade.completeDeliveryWithPodFallback(orderId, sigPath, photoPath, notes);
                    if (completed) {
                        log("✅ Delivery completed via DB fallback for order " + orderId);
                        refreshPodTable();
                    } else {
                        log("❌ Error: " + ex.getMessage());
                    }
                }
            });

            for (JComponent c : new JComponent[]{orderIdFld, sigFileFld, photoFileFld, notesFld, completeBtn}) {
                card.add(c);
            }
            return card;
        }

        private JPanel buildQueryPODCard() {
            JPanel card = makeCard("🔍 Query POD Record");

            JTextField queryFld = field("Order ID");
            JTextArea  podDetail = new JTextArea(6, 0);
            podDetail.setEditable(false);
            podDetail.setBackground(LuxuryTheme.BG_CARD);
            podDetail.setForeground(LuxuryTheme.TEXT_PRIMARY);
            podDetail.setFont(new Font("Monospaced", Font.PLAIN, 12));
            podDetail.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            JScrollPane podDetailScroll = new JScrollPane(podDetail);
            podDetailScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
            podDetailScroll.setPreferredSize(new Dimension(0, 120));
            podDetailScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

            JButton queryBtn = LuxuryTheme.ghostButton("Fetch POD");
            queryBtn.addActionListener(e -> {
                DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
                String orderId = queryFld.getText().trim();
                try {
                    PODRecord pod = ds == null ? null : ds.getPOD(orderId);
                    if (pod != null) {
                        podDetail.setText(
                            "Order ID   : " + pod.getOrderId()        + "\n" +
                            "Rider ID   : " + pod.getRiderId()        + "\n" +
                            "Signature  : " + pod.getSignatureUrl()   + "\n" +
                            "Photo      : " + pod.getPhotoUrl()       + "\n" +
                            "Notes      : " + pod.getNotes()          + "\n" +
                            "Submitted  : " + pod.getSubmittedAt()
                        );
                    } else {
                        Object[] row = facade.getPodDetailsForOrder(orderId);
                        if (row != null) {
                            podDetail.setText(
                                "Order ID   : " + String.valueOf(row[0]) + "\n" +
                                "Rider ID   : " + String.valueOf(row[1]) + "\n" +
                                "Signature  : " + String.valueOf(row[2]) + "\n" +
                                "Photo      : " + String.valueOf(row[3]) + "\n" +
                                "Notes      : " + String.valueOf(row[4]) + "\n" +
                                "Submitted  : " + String.valueOf(row[5])
                            );
                        } else {
                            podDetail.setText("No POD found for order: " + orderId);
                        }
                    }
                } catch (Exception ex) {
                    Object[] row = facade.getPodDetailsForOrder(orderId);
                    if (row != null) {
                        podDetail.setText(
                            "Order ID   : " + String.valueOf(row[0]) + "\n" +
                            "Rider ID   : " + String.valueOf(row[1]) + "\n" +
                            "Signature  : " + String.valueOf(row[2]) + "\n" +
                            "Photo      : " + String.valueOf(row[3]) + "\n" +
                            "Notes      : " + String.valueOf(row[4]) + "\n" +
                            "Submitted  : " + String.valueOf(row[5])
                        );
                    } else {
                        podDetail.setText("Error: " + ex.getMessage());
                    }
                }
            });

            for (JComponent c : new JComponent[]{queryFld, queryBtn, podDetailScroll}) {
                card.add(c);
            }
            return card;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 5 — ETA & CONTROLS
    // ─────────────────────────────────────────────────────────────────────────

    class ETAControlsTab extends JPanel {

        private final JTextArea logArea;

        ETAControlsTab() {
            setBackground(LuxuryTheme.BG_PANEL);
            setLayout(new BorderLayout(0, 0));
            setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

            JLabel title = LuxuryTheme.subHeading("ETA, GPS Simulation & Status History");

            JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
            cards.setBackground(LuxuryTheme.BG_PANEL);
            cards.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

            logArea = new JTextArea(8, 0);
            logArea.setEditable(false);
            logArea.setBackground(LuxuryTheme.BG_CARD);
            logArea.setForeground(LuxuryTheme.TEXT_PRIMARY);
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            logArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            JScrollPane logScroll = new JScrollPane(logArea);
            logScroll.setBorder(new LineBorder(LuxuryTheme.BORDER_SUBTLE));

            cards.add(buildETACard());
            cards.add(buildGPSSimCard());
            cards.add(buildStatusHistoryCard());

            add(title,     BorderLayout.NORTH);
            add(cards,     BorderLayout.CENTER);

            JPanel logPanel = new JPanel(new BorderLayout(0, 6));
            logPanel.setBackground(LuxuryTheme.BG_PANEL);
            logPanel.add(LuxuryTheme.subHeading("Output Log"), BorderLayout.NORTH);
            logPanel.add(logScroll, BorderLayout.CENTER);
            add(logPanel,  BorderLayout.SOUTH);
        }

        private void log(String msg) {
            String ts = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            logArea.append("[" + ts + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }

        private JPanel buildETACard() {
            JPanel card = makeCard("⏱ ETA Query");

            JTextField orderIdFld = field("Order ID");

            JButton etaBtn = LuxuryTheme.primaryButton("Get Latest ETA");
            etaBtn.addActionListener(e -> {
                DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
                String orderId = orderIdFld.getText().trim();
                try {
                    ETARecord eta = ds == null ? null : ds.getLatestETA(orderId);
                    if (eta != null) {
                        log("⏱ ETA for " + orderId +
                            ": " + eta.getRemainingTimeMinutes() + " min" +
                            " (as of " + eta.getCalculatedAt() + ")");
                    } else {
                        Integer etaMins = facade.getLatestEtaMinutesForOrder(orderId);
                        if (etaMins != null) {
                            log("⏱ ETA for " + orderId + ": " + etaMins + " min (DB)");
                        } else {
                            log("ℹ No ETA calculated yet for " + orderId);
                        }
                    }
                } catch (Exception ex) {
                    Integer etaMins = facade.getLatestEtaMinutesForOrder(orderId);
                    if (etaMins != null) {
                        log("⏱ ETA for " + orderId + ": " + etaMins + " min (DB)");
                    } else {
                        log("❌ Error: " + ex.getMessage());
                    }
                }
            });

            for (JComponent c : new JComponent[]{orderIdFld, etaBtn}) {
                card.add(c);
            }
            return card;
        }

        private JPanel buildGPSSimCard() {
            JPanel card = makeCard("📡 Simulate GPS Ping");

            JTextField deviceIdFld = field("Device ID");
            JTextField orderIdFld  = field("Order ID");
            JTextField latFld      = field("Latitude");
            JTextField lngFld      = field("Longitude");

            JButton pingBtn = LuxuryTheme.primaryButton("Send GPS Ping");
            pingBtn.addActionListener(e -> {
                DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
                String deviceId = deviceIdFld.getText().trim();
                String orderId = orderIdFld.getText().trim();
                try {
                    double lat = Double.parseDouble(latFld.getText().trim());
                    double lng = Double.parseDouble(lngFld.getText().trim());
                    GPSPing ping = ds == null ? null : ds.processLocationUpdate(deviceId, orderId, lat, lng);
                    boolean savedViaDb = false;
                    if (ping == null) {
                        savedViaDb = facade.saveGpsPingAndMarkTransit(deviceId, orderId, lat, lng);
                    }
                    if (ping != null || savedViaDb) {
                        double outLat = (ping != null) ? ping.getLatitude() : lat;
                        double outLng = (ping != null) ? ping.getLongitude() : lng;
                        log("📍 GPS ping processed: lat=" + outLat +
                            " lng=" + outLng + (savedViaDb ? " (DB)" : ""));
                        liveTrackingTab.pushGPSRow(
                            orderId,
                            deviceId,
                            lat, lng, "IN_TRANSIT"
                        );
                    } else {
                        log("⚠ GPS ping could not be saved.");
                    }
                } catch (NumberFormatException ex) {
                    log("❌ Invalid lat/lng — must be decimal numbers.");
                } catch (Exception ex) {
                    try {
                        double lat = Double.parseDouble(latFld.getText().trim());
                        double lng = Double.parseDouble(lngFld.getText().trim());
                        if (facade.saveGpsPingAndMarkTransit(deviceId, orderId, lat, lng)) {
                            log("📍 GPS ping processed via DB fallback: lat=" + lat + " lng=" + lng);
                            liveTrackingTab.pushGPSRow(orderId, deviceId, lat, lng, "IN_TRANSIT");
                            return;
                        }
                    } catch (Exception ignored) { }
                    log("❌ Error: " + ex.getMessage());
                }
            });

            for (JComponent c : new JComponent[]{deviceIdFld, orderIdFld, latFld, lngFld, pingBtn}) {
                card.add(c);
            }
            return card;
        }

        private JPanel buildStatusHistoryCard() {
            JPanel card = makeCard("📋 Status History");

            JTextField orderIdFld = field("Order ID");
            JTextArea  histArea   = new JTextArea(6, 0);
            histArea.setEditable(false);
            histArea.setBackground(LuxuryTheme.BG_CARD);
            histArea.setForeground(LuxuryTheme.TEXT_PRIMARY);
            histArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            histArea.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

            JButton histBtn = LuxuryTheme.ghostButton("Fetch History");
            histBtn.addActionListener(e -> {
                DeliveryMonitoringFacadeDB ds = facade.getDeliverySystem();
                String orderId = orderIdFld.getText().trim();
                try {
                    List<?> history = ds == null ? null : ds.getStatusHistory(orderId);
                    if (history == null || history.isEmpty()) {
                        List<String> dbHistory = facade.getDeliveryStatusHistoryLines(orderId);
                        if (dbHistory.isEmpty()) {
                            histArea.setText("No status history for " + orderId);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            for (String line : dbHistory) sb.append(line).append("\n");
                            histArea.setText(sb.toString());
                        }
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (Object entry : history) {
                            sb.append(entry.toString()).append("\n");
                        }
                        histArea.setText(sb.toString());
                    }
                } catch (Exception ex) {
                    List<String> dbHistory = facade.getDeliveryStatusHistoryLines(orderId);
                    if (dbHistory.isEmpty()) {
                        histArea.setText("Error: " + ex.getMessage());
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (String line : dbHistory) sb.append(line).append("\n");
                        histArea.setText(sb.toString());
                    }
                }
            });

            for (JComponent c : new JComponent[]{orderIdFld, histBtn, new JScrollPane(histArea)}) {
                card.add(c);
            }
            return card;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHARED HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String createOrderViaDbFallback(String customerId, String pickupAddress, String dropoffAddress,
                                            double pickupLat, double pickupLng, double dropoffLat, double dropoffLng) {
        return facade.createDeliveryOrderWithGeo(
            customerId, pickupAddress, dropoffAddress, pickupLat, pickupLng, dropoffLat, dropoffLng
        );
    }

    /** Creates a styled card panel with a title label and BoxLayout. */
    private static JPanel makeCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(LuxuryTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(LuxuryTheme.BORDER_SUBTLE),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JLabel lbl = new JLabel(title);
        lbl.setFont(LuxuryTheme.FONT_SUBHEAD);
        lbl.setForeground(LuxuryTheme.TEXT_GOLD);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lbl);
        return card;
    }

    /** Creates a styled, left-aligned text field with placeholder text. */
    private static JTextField field(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setBackground(LuxuryTheme.BG_INPUT);
        f.setForeground(LuxuryTheme.TEXT_SECOND);
        f.setFont(LuxuryTheme.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(LuxuryTheme.BORDER_SUBTLE),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Clear placeholder on focus
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(LuxuryTheme.TEXT_PRIMARY); }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(LuxuryTheme.TEXT_SECOND); }
            }
        });
        return f;
    }
}
