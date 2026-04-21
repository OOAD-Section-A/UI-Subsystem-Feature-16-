package scm.ui.logistics;

import scm.ui.model.*;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * C-06 — Transport & Logistics UI
 * Tabs: Deliveries | Shipments | Live Tracking Map (simulated)
 */
public class LogisticsPanel extends JPanel {

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();

    public LogisticsPanel() {
        setLayout(new BorderLayout());
        setBackground(LuxuryTheme.BG_PANEL);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LuxuryTheme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 0, 28));
        header.add(LuxuryTheme.sectionTitle("Transport  &  Logistics"), BorderLayout.WEST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(LuxuryTheme.BG_PANEL);
        tabs.setFont(LuxuryTheme.FONT_SUBHEAD);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        tabs.addTab("🚚 Deliveries",      new DeliveryCrudPanel());
        tabs.addTab("📦 Shipments",       new ShipmentCrudPanel());
        tabs.addTab("🗺  Live Map View",   new LiveMapPanel());

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Deliveries
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class DeliveryCrudPanel extends CrudPanel {
        DeliveryCrudPanel() {
            init("Delivery Orders", new String[]{"Delivery ID","Order ID","Customer","Status","Type","Cost","Agent","Warehouse","Created"});
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (DeliveryOrder d : facade.getAllDeliveries())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{
                    d.getDeliveryId(), d.getOrderId(), d.getCustomerId(), d.getDeliveryStatus(),
                    d.getDeliveryType(), AppUtils.currency(d.getDeliveryCost()),
                    d.getAssignedAgent(), d.getWarehouseId(), d.getCreatedAt()}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Create Delivery Order", 540, 500);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("DEL"));
            JTextField ordF  = LuxuryTheme.textField("ORD-001");
            JTextField custF = LuxuryTheme.textField("CUST-001");
            JTextArea addrTA = LuxuryTheme.textArea(3, 30);
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"PENDING","DISPATCHED","IN_TRANSIT","DELIVERED","FAILED"});
            JComboBox<String> typCB = LuxuryTheme.comboBox(new String[]{"STANDARD","EXPRESS","SAME_DAY","DROP_SHIP"});
            JSpinner costSp = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 999999.0, 0.01));
            JTextField agentF = LuxuryTheme.textField("AGENT-001");
            JTextField whF    = LuxuryTheme.textField("WH-001");
            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Delivery Details")
                .addField("Delivery ID *", idF).addField("Order ID *", ordF).addField("Customer ID *", custF)
                .addField("Delivery Address *", new JScrollPane(addrTA))
                .addField("Status", stCB).addField("Type", typCB).addField("Delivery Cost (₹)", costSp)
                .addField("Assigned Agent", agentF).addField("Warehouse ID *", whF).build();
            addDialogButtons(d, form, () -> {
                DeliveryOrder del = new DeliveryOrder();
                del.setDeliveryId(idF.getText().trim()); del.setOrderId(ordF.getText().trim()); del.setCustomerId(custF.getText().trim());
                del.setDeliveryAddress(addrTA.getText()); del.setDeliveryStatus((String)stCB.getSelectedItem());
                del.setDeliveryType((String)typCB.getSelectedItem()); del.setDeliveryCost((double)costSp.getValue());
                del.setAssignedAgent(agentF.getText().trim()); del.setWarehouseId(whF.getText().trim());
                int r = facade.createDelivery(del);
                if (r > 0) { d.dispose(); loadDataAsync(); } else AppUtils.showError(d, "Failed.");
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            DeliveryOrder del = facade.getAllDeliveries().stream().filter(dd -> dd.getDeliveryId().equals(id)).findFirst().orElse(null);
            if (del == null) return;
            JDialog d = createDialog("Edit Delivery: " + id, 440, 300);
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"PENDING","DISPATCHED","IN_TRANSIT","DELIVERED","FAILED"});
            stCB.setSelectedItem(del.getDeliveryStatus());
            JTextField agentF = LuxuryTheme.textField(del.getAssignedAgent());
            JSpinner costSp = new JSpinner(new SpinnerNumberModel(del.getDeliveryCost(), 0.0, 999999.0, 0.01));
            JPanel form = new CrudPanel.FormBuilder()
                .addField("Status", stCB).addField("Assigned Agent", agentF).addField("Cost (₹)", costSp).build();
            addDialogButtons(d, form, () -> {
                del.setDeliveryStatus((String)stCB.getSelectedItem()); del.setAssignedAgent(agentF.getText().trim()); del.setDeliveryCost((double)costSp.getValue());
                facade.updateDelivery(del); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deleteDelivery((String)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Shipments
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class ShipmentCrudPanel extends CrudPanel {
        ShipmentCrudPanel() {
            init("Shipments", new String[]{"Shipment ID","Order ID","Status","Priority","Weight","Carrier","Tracking ID","Cost"});
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (Shipment s : facade.getAllShipments())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{
                    s.getShipmentId(), s.getOrderId(), s.getShipmentStatus(), s.getShippingPriority(),
                    s.getPackageWeight() + " kg", s.getCarrierId(), s.getTrackingId(),
                    AppUtils.currency(s.getCalculatedCost())}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Create Shipment", 560, 500);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("SHP"));
            JTextField ordF  = LuxuryTheme.textField("ORD-001");
            JTextArea origTA = LuxuryTheme.textArea(2, 30);
            JTextArea destTA = LuxuryTheme.textArea(2, 30);
            JSpinner wgtSp   = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 9999.0, 0.1));
            JComboBox<String> priCB = LuxuryTheme.comboBox(new String[]{"STANDARD","EXPRESS","PRIORITY","OVERNIGHT"});
            JComboBox<String> stCB  = LuxuryTheme.comboBox(new String[]{"PENDING","DISPATCHED","IN_TRANSIT","DELIVERED","RETURNED"});
            JTextField carrF = LuxuryTheme.textField("CARRIER-001");
            JTextField trkF  = LuxuryTheme.textField("TRK-001");
            JSpinner costSp  = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 999999.0, 0.01));
            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Shipment Details")
                .addField("Shipment ID *", idF).addField("Order ID *", ordF)
                .addField("Origin Address *", new JScrollPane(origTA)).addField("Destination *", new JScrollPane(destTA))
                .addField("Weight (kg) *", wgtSp).addField("Priority", priCB).addField("Status", stCB)
                .addField("Carrier ID", carrF).addField("Tracking ID", trkF).addField("Estimated Cost (₹)", costSp).build();
            addDialogButtons(d, form, () -> {
                Shipment s = new Shipment();
                s.setShipmentId(idF.getText().trim()); s.setOrderId(ordF.getText().trim());
                s.setOriginAddress(origTA.getText()); s.setDestinationAddress(destTA.getText());
                s.setPackageWeight((double)wgtSp.getValue()); s.setShippingPriority((String)priCB.getSelectedItem());
                s.setShipmentStatus((String)stCB.getSelectedItem()); s.setCarrierId(carrF.getText().trim());
                s.setTrackingId(trkF.getText().trim()); s.setCalculatedCost((double)costSp.getValue());
                facade.createShipment(s); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            Shipment s = facade.getAllShipments().stream().filter(sh -> sh.getShipmentId().equals(id)).findFirst().orElse(null);
            if (s == null) return;
            JDialog d = createDialog("Edit Shipment: " + id, 440, 280);
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"PENDING","DISPATCHED","IN_TRANSIT","DELIVERED","RETURNED"});
            stCB.setSelectedItem(s.getShipmentStatus());
            JTextField carrF = LuxuryTheme.textField(s.getCarrierId());
            JTextField trkF  = LuxuryTheme.textField(s.getTrackingId());
            JSpinner costSp  = new JSpinner(new SpinnerNumberModel(s.getCalculatedCost(), 0.0, 999999.0, 0.01));
            JPanel form = new CrudPanel.FormBuilder()
                .addField("Status", stCB).addField("Carrier ID", carrF).addField("Tracking ID", trkF).addField("Cost (₹)", costSp).build();
            addDialogButtons(d, form, () -> {
                s.setShipmentStatus((String)stCB.getSelectedItem()); s.setCarrierId(carrF.getText().trim());
                s.setTrackingId(trkF.getText().trim()); s.setCalculatedCost((double)costSp.getValue());
                facade.updateShipment(s); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deleteShipment((String)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Live Map (real shipment GPS tracking)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class LiveMapPanel extends JPanel {
        private final double MIN_LAT = 6.5;
        private final double MAX_LAT = 37.5;
        private final double MIN_LNG = 67.0;
        private final double MAX_LNG = 98.5;
        private final Map<String, GeoPoint> CITY_COORDS = buildCityCoords();

        private final JPanel mapCanvas;
        private final JLabel infoLabel;
        private final JLabel selectedShipmentLabel;
        private final JLabel selectedStatusLabel;
        private final JLabel selectedVehicleLabel;
        private final JLabel selectedSourceLabel;
        private final JLabel selectedDestinationLabel;
        private final JLabel selectedCoordinateLabel;
        private List<ShipmentMapPoint> livePoints = List.of();
        private final List<MarkerHit> markerHits = new ArrayList<>();
        private ShipmentMapPoint selectedPoint;
        private boolean loading = false;

        LiveMapPanel() {
            setBackground(LuxuryTheme.BG_PANEL);
            setLayout(new BorderLayout(0, 10));

            JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            topBar.setBackground(LuxuryTheme.BG_PANEL);
            topBar.setBorder(BorderFactory.createEmptyBorder(10, 28, 0, 28));
            topBar.add(LuxuryTheme.subHeading("Live Shipment Map"));
            JButton refreshBtn = LuxuryTheme.primaryButton("↻ Refresh");
            refreshBtn.addActionListener(e -> loadLivePoints());
            topBar.add(refreshBtn);
            infoLabel = new JLabel("Loading live shipment GPS data...");
            infoLabel.setForeground(LuxuryTheme.TEXT_SECOND);
            infoLabel.setFont(LuxuryTheme.FONT_SMALL);
            topBar.add(infoLabel);

            JPanel detailsCard = new JPanel(new GridLayout(2, 3, 14, 6));
            detailsCard.setBackground(LuxuryTheme.BG_CARD);
            detailsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LuxuryTheme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            selectedShipmentLabel = makeDetailLabel("Shipment", "Select a marker");
            selectedStatusLabel = makeDetailLabel("Status", "—");
            selectedVehicleLabel = makeDetailLabel("Vehicle", "—");
            selectedSourceLabel = makeDetailLabel("Source", "—");
            selectedDestinationLabel = makeDetailLabel("Destination", "—");
            selectedCoordinateLabel = makeDetailLabel("Current GPS", "—");
            detailsCard.add(selectedShipmentLabel);
            detailsCard.add(selectedStatusLabel);
            detailsCard.add(selectedVehicleLabel);
            detailsCard.add(selectedSourceLabel);
            detailsCard.add(selectedDestinationLabel);
            detailsCard.add(selectedCoordinateLabel);

            JPanel topSection = new JPanel(new BorderLayout(0, 8));
            topSection.setBackground(LuxuryTheme.BG_PANEL);
            topSection.add(topBar, BorderLayout.NORTH);
            topSection.add(wrapDetailsCard(detailsCard), BorderLayout.CENTER);

            mapCanvas = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    drawMap((Graphics2D) g, getWidth(), getHeight());
                }
            };
            mapCanvas.setBackground(new Color(15, 20, 40));
            mapCanvas.setPreferredSize(new Dimension(0, 480));
            mapCanvas.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    MarkerHit hit = findMarkerAt(e.getX(), e.getY());
                    if (hit != null) {
                        selectedPoint = hit.point;
                        infoLabel.setText("Selected " + hit.point.getShipmentId() + ". Click another marker to view details.");
                        updateSelectionDetails();
                    }
                    mapCanvas.repaint();
                }
            });
            mapCanvas.addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    mapCanvas.setCursor(findMarkerAt(e.getX(), e.getY()) != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                }
            });

            // legend
            JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
            legend.setBackground(LuxuryTheme.BG_PANEL);
            legend.setBorder(BorderFactory.createEmptyBorder(0, 28, 10, 28));
            legend.add(makeDot(new Color(120, 170, 255))); legend.add(new JLabel("Source / Warehouse") {{ setForeground(LuxuryTheme.TEXT_SECOND); }});
            legend.add(makeDot(new Color(255, 175, 95)));  legend.add(new JLabel("Destination") {{ setForeground(LuxuryTheme.TEXT_SECOND); }});
            legend.add(makeDot(LuxuryTheme.ACCENT_TEAL));  legend.add(new JLabel("Vehicle Position") {{ setForeground(LuxuryTheme.TEXT_SECOND); }});

            add(topSection, BorderLayout.NORTH);
            add(mapCanvas, BorderLayout.CENTER);
            add(legend, BorderLayout.SOUTH);

            loadLivePoints();
        }

        private void loadLivePoints() {
            if (loading) return;
            loading = true;
            infoLabel.setText("Loading live shipment GPS data...");
            mapCanvas.repaint();
            SwingWorker<List<ShipmentMapPoint>, Void> worker = new SwingWorker<>() {
                @Override protected List<ShipmentMapPoint> doInBackground() {
                    return facade.getShipmentLiveMapPoints();
                }

                @Override protected void done() {
                    try {
                        livePoints = get();
                        if (livePoints.isEmpty()) {
                            selectedPoint = null;
                            infoLabel.setText("No active in-transit shipments found.");
                        } else {
                            if (selectedPoint == null) {
                                selectedPoint = livePoints.get(0);
                            } else {
                                ShipmentMapPoint currentSelection = selectedPoint;
                                selectedPoint = livePoints.stream()
                                    .filter(p -> p.getShipmentId() != null && p.getShipmentId().equals(currentSelection.getShipmentId()))
                                    .findFirst()
                                    .orElse(livePoints.get(0));
                            }
                            long liveGpsCount = livePoints.stream().filter(ShipmentMapPoint::isLiveGpsAvailable).count();
                            infoLabel.setText("Showing " + livePoints.size() + " active shipment(s), " + liveGpsCount + " with live GPS.");
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        infoLabel.setText("Live map refresh interrupted.");
                    } catch (java.util.concurrent.ExecutionException ex) {
                        infoLabel.setText("Failed to load live shipment map data.");
                    } finally {
                        updateSelectionDetails();
                        loading = false;
                        mapCanvas.repaint();
                    }
                }
            };
            worker.execute();
        }

        private void drawMap(Graphics2D g2, int panelWidth, int panelHeight) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int mapX = 28;
            int mapY = 10;
            int w = panelWidth - 56;
            int h = panelHeight - 40;

            drawMapBackdrop(g2, mapX, mapY, w, h);
            if (loading) {
                g2.setColor(LuxuryTheme.TEXT_SECOND);
                g2.setFont(LuxuryTheme.FONT_SUBHEAD);
                g2.drawString("Loading live GPS points...", 42, 40);
                return;
            }
            if (livePoints.isEmpty()) {
                g2.setColor(LuxuryTheme.TEXT_SECOND);
                g2.setFont(LuxuryTheme.FONT_SUBHEAD);
                g2.drawString("No live GPS data available for current shipments.", 42, 40);
                return;
            }

            markerHits.clear();
            Map<String, Integer> stackByCell = new HashMap<>();
            for (ShipmentMapPoint point : livePoints) {
                GeoPoint source = lookupAddressPoint(point.getOriginAddress());
                GeoPoint destination = lookupAddressPoint(point.getDestinationAddress());
                GeoPoint current = resolveCurrentPoint(point, source, destination);
                if (current == null) continue;
                boolean selected = selectedPoint != null && selectedPoint.getShipmentId() != null
                    && selectedPoint.getShipmentId().equals(point.getShipmentId());

                if (source != null) {
                    drawRouteLine(g2, source, current, mapX, mapY, w, h, selected ? new Color(140, 190, 255, 210) : new Color(120, 170, 255, 120));
                    drawSourceMarker(g2, source, mapX, mapY, w, h, selected);
                }
                if (destination != null) {
                    drawRouteLine(g2, current, destination, mapX, mapY, w, h, selected ? new Color(255, 190, 120, 210) : new Color(255, 175, 95, 120));
                    drawDestinationMarker(g2, destination, mapX, mapY, w, h, selected);
                }

                int x = projectX(current.lng, mapX, w);
                int y = projectY(current.lat, mapY, h);
                String cell = (x / 12) + ":" + (y / 12);
                int stackIndex = stackByCell.getOrDefault(cell, 0);
                stackByCell.put(cell, stackIndex + 1);
                x += (stackIndex % 3) * 10;
                y -= (stackIndex / 3) * 10;

                Color col = statusColor(point.getShipmentStatus());
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 60));
                g2.fillOval(x-12, y-12, 24, 24);
                g2.setColor(col);
                g2.fillOval(x-6, y-6, 12, 12);
                if (selected) {
                    g2.setColor(new Color(255, 255, 255, 220));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(x - 9, y - 9, 18, 18);
                }
                g2.setColor(LuxuryTheme.TEXT_PRIMARY);
                g2.setFont(LuxuryTheme.FONT_SMALL);
                if (selected) {
                    String label = point.getShipmentId();
                    if (point.getVehicleId() != null && !point.getVehicleId().isBlank()) {
                        label += " (" + point.getVehicleId() + ")";
                    }
                    g2.drawString(label, x + 10, y + 4);
                }
                markerHits.add(new MarkerHit(point, x, y, 10));
            }
        }

        private void drawMapBackdrop(Graphics2D g2, int x, int y, int w, int h) {
            GradientPaint bg = new GradientPaint(x, y, new Color(16, 26, 55), x, y + h, new Color(10, 18, 40));
            g2.setPaint(bg);
            g2.fillRect(x, y, w, h);

            g2.setColor(new Color(35, 50, 84));
            g2.setStroke(new BasicStroke(0.6f));
            for (int i = 0; i < 8; i++) {
                g2.drawLine(x, y + i * h / 7, x + w, y + i * h / 7);
                g2.drawLine(x + i * w / 7, y, x + i * w / 7, y + h);
            }

            Path2D india = new Path2D.Double();
            double[][] poly = new double[][]{
                {68.2, 23.5}, {69.5, 22.0}, {70.0, 20.2}, {71.0, 19.2}, {72.0, 18.0}, {72.8, 16.0},
                {73.4, 14.5}, {74.0, 12.8}, {75.8, 10.5}, {77.6, 8.4}, {79.8, 9.1}, {81.4, 11.4},
                {83.0, 14.6}, {84.8, 18.2}, {87.2, 21.8}, {88.9, 23.2}, {90.0, 25.2}, {91.5, 26.6},
                {92.8, 27.8}, {94.0, 26.8}, {95.5, 27.4}, {96.4, 28.7}, {95.2, 30.2}, {92.8, 28.7},
                {90.0, 29.0}, {88.4, 27.7}, {85.3, 28.6}, {82.0, 30.2}, {78.2, 31.8}, {75.2, 33.5},
                {73.5, 35.0}, {71.5, 33.5}, {70.3, 30.2}, {69.0, 27.2}, {68.2, 23.5}
            };
            for (int i = 0; i < poly.length; i++) {
                int px = projectX(poly[i][0], x, w);
                int py = projectY(poly[i][1], y, h);
                if (i == 0) india.moveTo(px, py); else india.lineTo(px, py);
            }

            g2.setColor(new Color(24, 60, 78, 150));
            g2.fill(india);
            g2.setColor(new Color(70, 130, 160, 190));
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(india);

            drawCityLayer(g2, x, y, w, h);
        }

        private void drawRouteLine(Graphics2D g2, GeoPoint from, GeoPoint to, int x, int y, int w, int h, Color color) {
            int x1 = projectX(from.lng, x, w);
            int y1 = projectY(from.lat, y, h);
            int x2 = projectX(to.lng, x, w);
            int y2 = projectY(to.lat, y, h);
            int mx = (x1 + x2) / 2;
            int my = (y1 + y2) / 2 - Math.max(12, Math.abs(x2 - x1) / 8);
            Stroke old = g2.getStroke();
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 4f}, 0f));
            g2.draw(new QuadCurve2D.Float(x1, y1, mx, my, x2, y2));
            g2.setStroke(old);
        }

        private void drawCityLayer(Graphics2D g2, int x, int y, int w, int h) {
            String[] cities = {"delhi", "mumbai", "bengaluru", "chennai", "kolkata", "hyderabad"};
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            for (String key : cities) {
                GeoPoint p = CITY_COORDS.get(key);
                if (p == null) continue;
                int px = projectX(p.lng, x, w);
                int py = projectY(p.lat, y, h);
                g2.setColor(new Color(170, 195, 225, 160));
                g2.fillOval(px - 2, py - 2, 4, 4);
                g2.setColor(new Color(150, 175, 210, 110));
                g2.drawString(Character.toUpperCase(key.charAt(0)) + key.substring(1), px + 4, py - 4);
            }
        }

        private GeoPoint resolveCurrentPoint(ShipmentMapPoint point, GeoPoint source, GeoPoint destination) {
            if (point.isLiveGpsAvailable()) return new GeoPoint(point.getLatitude(), point.getLongitude());
            if (source != null && destination != null) {
                double t = deterministicProgress(point.getShipmentId());
                double lat = source.lat + (destination.lat - source.lat) * t;
                double lng = source.lng + (destination.lng - source.lng) * t;
                return new GeoPoint(lat, lng);
            }
            if (source != null) return source;
            if (destination != null) return destination;
            return null;
        }

        private double deterministicProgress(String shipmentId) {
            int hash = shipmentId == null ? 0 : Math.abs(shipmentId.hashCode());
            return 0.2 + (hash % 60) / 100.0; // 0.20 to 0.79
        }

        private void drawSourceMarker(Graphics2D g2, GeoPoint p, int x, int y, int w, int h, boolean selected) {
            int px = projectX(p.lng, x, w);
            int py = projectY(p.lat, y, h);
            g2.setColor(selected ? new Color(150, 200, 255) : new Color(120, 170, 255));
            g2.fillRect(px - 4, py - 4, 8, 8);
            g2.setColor(new Color(190, 225, 255));
            g2.drawRect(px - 4, py - 4, 8, 8);
        }

        private void drawDestinationMarker(Graphics2D g2, GeoPoint p, int x, int y, int w, int h, boolean selected) {
            int px = projectX(p.lng, x, w);
            int py = projectY(p.lat, y, h);
            Polygon diamond = new Polygon(
                new int[]{px, px + 5, px, px - 5},
                new int[]{py - 5, py, py + 5, py},
                4
            );
            g2.setColor(selected ? new Color(255, 200, 130) : new Color(255, 175, 95));
            g2.fillPolygon(diamond);
            g2.setColor(new Color(255, 210, 160));
            g2.drawPolygon(diamond);
        }

        private int projectX(double lng, int x, int w) {
            return x + (int)(((lng - MIN_LNG) / (MAX_LNG - MIN_LNG)) * w);
        }

        private int projectY(double lat, int y, int h) {
            return y + (int)((1 - (lat - MIN_LAT) / (MAX_LAT - MIN_LAT)) * h);
        }

        private GeoPoint lookupAddressPoint(String address) {
            if (address == null || address.isBlank()) return null;
            String normalized = address.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, GeoPoint> entry : CITY_COORDS.entrySet()) {
                if (normalized.contains(entry.getKey())) return entry.getValue();
            }
            return null;
        }

        private Map<String, GeoPoint> buildCityCoords() {
            Map<String, GeoPoint> map = new LinkedHashMap<>();
            map.put("bengaluru", new GeoPoint(12.9716, 77.5946));
            map.put("bangalore", new GeoPoint(12.9716, 77.5946));
            map.put("mumbai", new GeoPoint(19.0760, 72.8777));
            map.put("delhi", new GeoPoint(28.6139, 77.2090));
            map.put("new delhi", new GeoPoint(28.6139, 77.2090));
            map.put("hyderabad", new GeoPoint(17.3850, 78.4867));
            map.put("chennai", new GeoPoint(13.0827, 80.2707));
            map.put("kolkata", new GeoPoint(22.5726, 88.3639));
            map.put("pune", new GeoPoint(18.5204, 73.8567));
            map.put("ahmedabad", new GeoPoint(23.0225, 72.5714));
            map.put("surat", new GeoPoint(21.1702, 72.8311));
            map.put("jaipur", new GeoPoint(26.9124, 75.7873));
            map.put("lucknow", new GeoPoint(26.8467, 80.9462));
            map.put("kanpur", new GeoPoint(26.4499, 80.3319));
            map.put("nagpur", new GeoPoint(21.1458, 79.0882));
            map.put("indore", new GeoPoint(22.7196, 75.8577));
            map.put("bhopal", new GeoPoint(23.2599, 77.4126));
            map.put("coimbatore", new GeoPoint(11.0168, 76.9558));
            map.put("kochi", new GeoPoint(9.9312, 76.2673));
            map.put("kochin", new GeoPoint(9.9312, 76.2673));
            map.put("trivandrum", new GeoPoint(8.5241, 76.9366));
            map.put("thiruvananthapuram", new GeoPoint(8.5241, 76.9366));
            map.put("visakhapatnam", new GeoPoint(17.6868, 83.2185));
            map.put("vijayawada", new GeoPoint(16.5062, 80.6480));
            map.put("patna", new GeoPoint(25.5941, 85.1376));
            map.put("bhubaneswar", new GeoPoint(20.2961, 85.8245));
            map.put("guwahati", new GeoPoint(26.1445, 91.7362));
            map.put("chandigarh", new GeoPoint(30.7333, 76.7794));
            map.put("ludhiana", new GeoPoint(30.9010, 75.8573));
            map.put("noida", new GeoPoint(28.5355, 77.3910));
            map.put("gurugram", new GeoPoint(28.4595, 77.0266));
            map.put("gurgaon", new GeoPoint(28.4595, 77.0266));
            return map;
        }

        private JPanel wrapDetailsCard(JPanel card) {
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBackground(LuxuryTheme.BG_PANEL);
            wrapper.setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 28));
            wrapper.add(card, BorderLayout.CENTER);
            return wrapper;
        }

        private JLabel makeDetailLabel(String title, String value) {
            JLabel label = new JLabel("<html><span style='color:#8fa3cf;'>" + title + ":</span> " + value + "</html>");
            label.setForeground(LuxuryTheme.TEXT_PRIMARY);
            label.setFont(LuxuryTheme.FONT_SMALL);
            return label;
        }

        private void updateSelectionDetails() {
            if (selectedPoint == null) {
                selectedShipmentLabel.setText("<html><span style='color:#8fa3cf;'>Shipment:</span> Select a marker</html>");
                selectedStatusLabel.setText("<html><span style='color:#8fa3cf;'>Status:</span> —</html>");
                selectedVehicleLabel.setText("<html><span style='color:#8fa3cf;'>Vehicle:</span> —</html>");
                selectedSourceLabel.setText("<html><span style='color:#8fa3cf;'>Source:</span> —</html>");
                selectedDestinationLabel.setText("<html><span style='color:#8fa3cf;'>Destination:</span> —</html>");
                selectedCoordinateLabel.setText("<html><span style='color:#8fa3cf;'>Current GPS:</span> —</html>");
                return;
            }
            selectedShipmentLabel.setText("<html><span style='color:#8fa3cf;'>Shipment:</span> " + safe(selectedPoint.getShipmentId()) + "</html>");
            selectedStatusLabel.setText("<html><span style='color:#8fa3cf;'>Status:</span> " + safe(selectedPoint.getShipmentStatus()) + "</html>");
            selectedVehicleLabel.setText("<html><span style='color:#8fa3cf;'>Vehicle:</span> " + safe(selectedPoint.getVehicleId()) + "</html>");
            selectedSourceLabel.setText("<html><span style='color:#8fa3cf;'>Source:</span> " + safe(selectedPoint.getOriginAddress()) + "</html>");
            selectedDestinationLabel.setText("<html><span style='color:#8fa3cf;'>Destination:</span> " + safe(selectedPoint.getDestinationAddress()) + "</html>");
            if (selectedPoint.isLiveGpsAvailable()) {
                selectedCoordinateLabel.setText(String.format(
                    Locale.ENGLISH,
                    "<html><span style='color:#8fa3cf;'>Current GPS:</span> %.4f, %.4f</html>",
                    selectedPoint.getLatitude(), selectedPoint.getLongitude()
                ));
            } else {
                selectedCoordinateLabel.setText("<html><span style='color:#8fa3cf;'>Current GPS:</span> Estimated route position</html>");
            }
        }

        private MarkerHit findMarkerAt(int x, int y) {
            for (int i = markerHits.size() - 1; i >= 0; i--) {
                MarkerHit hit = markerHits.get(i);
                int dx = x - hit.x;
                int dy = y - hit.y;
                if ((dx * dx + dy * dy) <= (hit.radius * hit.radius)) return hit;
            }
            return null;
        }

        private String safe(String value) {
            if (value == null || value.isBlank()) return "N/A";
            return value;
        }

        private Color statusColor(String status) {
            if (status == null) return LuxuryTheme.ACCENT_TEAL;
            String normalized = status.trim().toUpperCase();
            if ("DELIVERED".equals(normalized)) return LuxuryTheme.SUCCESS;
            if ("PENDING".equals(normalized) || "STAGED".equals(normalized) || "DISPATCHED".equals(normalized)) {
                return LuxuryTheme.WARNING;
            }
            return LuxuryTheme.ACCENT_TEAL;
        }

        private JLabel makeDot(Color c) {
            JLabel lbl = new JLabel("●");
            lbl.setForeground(c);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            return lbl;
        }

        private class GeoPoint {
            final double lat;
            final double lng;
            GeoPoint(double lat, double lng) { this.lat = lat; this.lng = lng; }
        }

        private class MarkerHit {
            final ShipmentMapPoint point;
            final int x;
            final int y;
            final int radius;

            MarkerHit(ShipmentMapPoint point, int x, int y, int radius) {
                this.point = point;
                this.x = x;
                this.y = y;
                this.radius = radius;
            }
        }
    }
}
