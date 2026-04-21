package scm.ui.dashboard;

import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.AppUtils;
import scm.ui.util.LuxuryTheme;
import scm.ui.model.Order;
import scm.ui.model.Shipment;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * C-03 — Dashboard & Analytics Screen
 * KPI cards, bar/line/pie charts (drawn manually with Java2D),
 * summary table, and export trigger.
 *
 * Inputs: kpiDataFeed, dateRangeFilter, warehouseId, chartDatasetArray, exportFormat
 * Outputs: KPI cards, bar/line/pie charts, summary table, PDF/CSV export trigger
 */
public class DashboardPanel extends JPanel {

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();

    private JLabel totalOrdersVal, totalRevenueVal, lowStockVal, shipmentsVal;
    private BarChartPanel barChart;
    private LineChartPanel lineChart;
    private PieChartPanel pieChart;
    private JTable summaryTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> dateRangeFilter;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(LuxuryTheme.BG_PANEL);
        buildUI();
        loadData();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(LuxuryTheme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 10, 28));

        JLabel title = LuxuryTheme.sectionTitle("Dashboard  &  Analytics");
        header.add(title, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);
        dateRangeFilter = LuxuryTheme.comboBox(new String[]{"Last 7 Days","Last 30 Days","Last 90 Days","This Year"});
        JButton refreshBtn = LuxuryTheme.primaryButton("↻  Refresh");
        JButton exportBtn = LuxuryTheme.ghostButton("↓  Export CSV");
        filterPanel.add(new JLabel("Period:") {{ setForeground(LuxuryTheme.TEXT_SECOND); setFont(LuxuryTheme.FONT_BODY); }});
        filterPanel.add(dateRangeFilter);
        filterPanel.add(refreshBtn);
        filterPanel.add(exportBtn);
        header.add(filterPanel, BorderLayout.EAST);

        refreshBtn.addActionListener(e -> loadData());
        dateRangeFilter.addActionListener(e -> loadData());
        exportBtn.addActionListener(e -> new AppUtils.CsvExportStrategy().export(summaryTable, this));

        // ── KPI Cards (4 cards) ───────────────────────────────────────────
        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiPanel.setBackground(LuxuryTheme.BG_PANEL);
        kpiPanel.setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));

        totalOrdersVal = new JLabel("—");
        totalOrdersVal.setFont(new Font("Segoe UI", Font.BOLD, 28));
        totalOrdersVal.setForeground(LuxuryTheme.ACCENT_BLUE);
        JPanel kpi1 = buildKpiCard("Total Orders", totalOrdersVal, LuxuryTheme.ACCENT_BLUE, "📋");

        totalRevenueVal = new JLabel("—");
        totalRevenueVal.setFont(new Font("Segoe UI", Font.BOLD, 28));
        totalRevenueVal.setForeground(LuxuryTheme.ACCENT_GOLD);
        JPanel kpi2 = buildKpiCard("Total Revenue", totalRevenueVal, LuxuryTheme.ACCENT_GOLD, "💰");

        lowStockVal = new JLabel("—");
        lowStockVal.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lowStockVal.setForeground(LuxuryTheme.DANGER);
        JPanel kpi3 = buildKpiCard("Low Stock Alerts", lowStockVal, LuxuryTheme.DANGER, "⚠");

        shipmentsVal = new JLabel("—");
        shipmentsVal.setFont(new Font("Segoe UI", Font.BOLD, 28));
        shipmentsVal.setForeground(LuxuryTheme.ACCENT_TEAL);
        JPanel kpi4 = buildKpiCard("Active Shipments", shipmentsVal, LuxuryTheme.ACCENT_TEAL, "🚚");

        kpiPanel.add(kpi1); kpiPanel.add(kpi2); kpiPanel.add(kpi3); kpiPanel.add(kpi4);

        // ── Charts row ────────────────────────────────────────────────────
        JPanel chartsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        chartsRow.setBackground(LuxuryTheme.BG_PANEL);
        chartsRow.setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));
        chartsRow.setPreferredSize(new Dimension(0, 260));

        barChart  = new BarChartPanel();
        lineChart = new LineChartPanel();
        pieChart  = new PieChartPanel();

        chartsRow.add(wrapChart(barChart,  "Orders by Month"));
        chartsRow.add(wrapChart(lineChart, "Revenue Trend"));
        chartsRow.add(wrapChart(pieChart,  "Order Status"));

        // ── Summary table ─────────────────────────────────────────────────
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(LuxuryTheme.BG_PANEL);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(10, 28, 20, 28));

        JLabel tableTitle = LuxuryTheme.subHeading("Recent Orders Summary");
        tableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        String[] cols = {"Order ID","Customer","Status","Amount","Payment","Channel","Date"};
        tableModel = AppUtils.tableModel(cols);
        summaryTable = new JTable(tableModel);
        JScrollPane sp = LuxuryTheme.styledTable(summaryTable);

        tablePanel.add(tableTitle, BorderLayout.NORTH);
        tablePanel.add(sp, BorderLayout.CENTER);

        // ── Assemble ──────────────────────────────────────────────────────
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(LuxuryTheme.BG_PANEL);
        mainContent.add(kpiPanel);
        mainContent.add(chartsRow);
        mainContent.add(tablePanel);

        JScrollPane scroll = new JScrollPane(mainContent);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(LuxuryTheme.BG_PANEL);
        scroll.getViewport().setBackground(LuxuryTheme.BG_PANEL);
        LuxuryTheme.styleScrollBar(scroll.getVerticalScrollBar());

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildKpiCard(String label, JLabel valueLabel, Color accent, String icon) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(LuxuryTheme.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),16,16));
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Double(1,1,getWidth()-2,getHeight()-2,16,16));
                g2.fillRoundRect(16, 0, 50, 4, 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0,8));
        card.setBorder(BorderFactory.createEmptyBorder(20,22,20,22));

        JLabel iconLabel = new JLabel(icon + "  " + label);
        iconLabel.setFont(LuxuryTheme.FONT_SMALL);
        iconLabel.setForeground(LuxuryTheme.TEXT_SECOND);

        card.add(iconLabel,   BorderLayout.NORTH);
        card.add(valueLabel,  BorderLayout.CENTER);
        return card;
    }

    private JPanel wrapChart(JPanel chart, String title) {
        JPanel wrapper = new JPanel(new BorderLayout(0,8));
        wrapper.setBackground(LuxuryTheme.BG_CARD);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(LuxuryTheme.BORDER_SUBTLE, 1, true),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        JLabel lbl = LuxuryTheme.subHeading(title);
        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(chart, BorderLayout.CENTER);
        return wrapper;
    }

    public void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int orderCount;
            double revenue;
            int lowStock;
            int shipments;
            List<Order> filteredOrders = new ArrayList<>();
            List<Order> allOrders = new ArrayList<>();
            int[] barData = new int[0];
            double[] lineData = new double[0];
            String[] trendLabels = new String[0];
            String[] pieLabels = new String[0];
            int[] pieValues = new int[0];
            Color[] pieColors = new Color[0];

            @Override protected Void doInBackground() {
                allOrders = facade.getAllOrders();
                LocalDateTime start = resolveRangeStart((String) dateRangeFilter.getSelectedItem());
                for (Order o : allOrders) {
                    LocalDateTime ts = parseOrderDate(o.getOrderDate());
                    if (ts != null && !ts.isBefore(start)) {
                        filteredOrders.add(o);
                    }
                }

                orderCount = filteredOrders.size();
                lowStock = facade.getLowStockItems().size();
                shipments = (int) facade.getAllShipments().stream()
                    .filter(s -> {
                        Shipment sh = (Shipment) s;
                        String st = sh.getShipmentStatus() == null ? "" : sh.getShipmentStatus().toUpperCase(Locale.ROOT);
                        return "IN_TRANSIT".equals(st) || "DISPATCHED".equals(st);
                    }).count();
                revenue = filteredOrders.stream().mapToDouble(Order::getTotalAmount).sum();

                TrendData trend = buildTrendData((String) dateRangeFilter.getSelectedItem(), filteredOrders);
                barData = trend.orderCounts;
                lineData = trend.revenueValues;
                trendLabels = trend.labels;

                StatusData status = buildStatusData(filteredOrders);
                pieLabels = status.labels;
                pieValues = status.values;
                pieColors = status.colors;
                return null;
            }

            @Override protected void done() {
                totalOrdersVal.setText(String.valueOf(orderCount));
                totalRevenueVal.setText(AppUtils.currency(revenue));
                lowStockVal.setText(String.valueOf(lowStock));
                shipmentsVal.setText(String.valueOf(shipments));

                barChart.setData(barData, trendLabels);
                lineChart.setData(lineData);
                pieChart.setData(pieLabels, pieValues, pieColors);

                // Fill table
                tableModel.setRowCount(0);
                int limit = Math.min(filteredOrders.size(), 50);
                for (int i = 0; i < limit; i++) {
                    Order o = filteredOrders.get(i);
                    tableModel.addRow(new Object[]{
                        o.getOrderId(), o.getCustomerId(), o.getOrderStatus(),
                        AppUtils.currency(o.getTotalAmount()), o.getPaymentStatus(),
                        o.getSalesChannel(), o.getOrderDate()
                    });
                }
            }
        };
        worker.execute();
    }

    private LocalDateTime resolveRangeStart(String range) {
        LocalDate today = LocalDate.now();
        if ("Last 30 Days".equals(range)) return today.minusDays(29).atStartOfDay();
        if ("Last 90 Days".equals(range)) return today.minusDays(89).atStartOfDay();
        if ("This Year".equals(range)) return LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
        return today.minusDays(6).atStartOfDay();
    }

    private TrendData buildTrendData(String range, List<Order> orders) {
        if ("Last 7 Days".equals(range)) {
            return buildDailyTrend(orders, 7);
        }
        if ("Last 30 Days".equals(range)) {
            return buildMonthlyTrend(orders, 2);
        }
        if ("Last 90 Days".equals(range)) {
            return buildMonthlyTrend(orders, 4);
        }
        int monthsInYear = LocalDate.now().getMonthValue();
        return buildMonthlyTrend(orders, Math.max(monthsInYear, 1));
    }

    private TrendData buildDailyTrend(List<Order> orders, int days) {
        LinkedHashMap<LocalDate, Bucket> buckets = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            buckets.put(today.minusDays(i), new Bucket());
        }
        for (Order o : orders) {
            LocalDateTime ts = parseOrderDate(o.getOrderDate());
            if (ts == null) continue;
            Bucket b = buckets.get(ts.toLocalDate());
            if (b != null) {
                b.count++;
                b.revenue += o.getTotalAmount();
            }
        }
        String[] labels = new String[buckets.size()];
        int[] counts = new int[buckets.size()];
        double[] revenueValues = new double[buckets.size()];
        int idx = 0;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
        for (Map.Entry<LocalDate, Bucket> e : buckets.entrySet()) {
            labels[idx] = e.getKey().format(f);
            counts[idx] = e.getValue().count;
            revenueValues[idx] = e.getValue().revenue;
            idx++;
        }
        return new TrendData(labels, counts, revenueValues);
    }

    private TrendData buildMonthlyTrend(List<Order> orders, int monthsBackInclusive) {
        LinkedHashMap<YearMonth, Bucket> buckets = new LinkedHashMap<>();
        YearMonth current = YearMonth.now();
        for (int i = monthsBackInclusive - 1; i >= 0; i--) {
            buckets.put(current.minusMonths(i), new Bucket());
        }
        for (Order o : orders) {
            LocalDateTime ts = parseOrderDate(o.getOrderDate());
            if (ts == null) continue;
            YearMonth ym = YearMonth.from(ts.toLocalDate());
            Bucket b = buckets.get(ym);
            if (b != null) {
                b.count++;
                b.revenue += o.getTotalAmount();
            }
        }
        String[] labels = new String[buckets.size()];
        int[] counts = new int[buckets.size()];
        double[] revenueValues = new double[buckets.size()];
        int idx = 0;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        for (Map.Entry<YearMonth, Bucket> e : buckets.entrySet()) {
            labels[idx] = e.getKey().format(f);
            counts[idx] = e.getValue().count;
            revenueValues[idx] = e.getValue().revenue;
            idx++;
        }
        return new TrendData(labels, counts, revenueValues);
    }

    private StatusData buildStatusData(List<Order> orders) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Order o : orders) {
            String status = o.getOrderStatus() == null ? "UNKNOWN" : o.getOrderStatus().trim().toUpperCase(Locale.ROOT);
            counts.put(status, counts.getOrDefault(status, 0) + 1);
        }
        if (counts.isEmpty()) {
            return new StatusData(
                new String[]{"NO DATA"},
                new int[]{1},
                new Color[]{LuxuryTheme.TEXT_MUTED}
            );
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        int maxSegments = 4;
        int use = Math.min(sorted.size(), maxSegments);
        String[] labels = new String[use];
        int[] values = new int[use];
        Color[] colors = new Color[use];
        Color[] palette = new Color[]{
            LuxuryTheme.ACCENT_BLUE, LuxuryTheme.SUCCESS, LuxuryTheme.DANGER, LuxuryTheme.ACCENT_GOLD
        };
        int otherTotal = 0;
        for (int i = 0; i < sorted.size(); i++) {
            if (i < maxSegments - 1 || sorted.size() <= maxSegments) {
                labels[i] = sorted.get(i).getKey();
                values[i] = sorted.get(i).getValue();
                colors[i] = palette[i % palette.length];
            } else {
                otherTotal += sorted.get(i).getValue();
            }
        }
        if (sorted.size() > maxSegments) {
            labels[maxSegments - 1] = "OTHER";
            values[maxSegments - 1] = otherTotal;
            colors[maxSegments - 1] = LuxuryTheme.ACCENT_GOLD;
        }
        return new StatusData(labels, values, colors);
    }

    private LocalDateTime parseOrderDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) { }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) { }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) { }
        return null;
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Embedded chart panels (Java2D — no external chart library needed)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    static class BarChartPanel extends JPanel {
        private int[] data = {40, 65, 55, 80, 70, 90};
        private String[] labels = {"Jan","Feb","Mar","Apr","May","Jun"};

        public void setData(int[] d, String[] l) { data = d; labels = l; repaint(); }

        BarChartPanel() { setOpaque(false); setPreferredSize(new Dimension(0, 160)); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.length == 0) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int pad = 30, chartW = w - pad*2, chartH = h - pad*2;
            int n = data.length;
            int max = 1; for (int v : data) max = Math.max(max, v);

            int barW = (chartW / n) - 6;

            for (int i = 0; i < n; i++) {
                float ratio = (float) data[i] / max;
                int barH = (int)(chartH * ratio);
                int x = pad + i * (chartW / n) + 3;
                int y = pad + chartH - barH;

                GradientPaint gp = new GradientPaint(x, y, LuxuryTheme.ACCENT_BLUE,
                    x, y + barH, new Color(40, 100, 200));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(x, y, barW, barH, 4, 4));

                g2.setColor(LuxuryTheme.TEXT_MUTED);
                g2.setFont(LuxuryTheme.FONT_SMALL);
                g2.drawString(i < labels.length ? labels[i] : "", x + barW/2 - 8, h - 8);
            }

            // baseline
            g2.setColor(LuxuryTheme.BORDER_SUBTLE);
            g2.drawLine(pad, h - pad, w - pad, h - pad);
            g2.dispose();
        }
    }

    static class LineChartPanel extends JPanel {
        private double[] data = {100, 150, 130, 200, 180, 250};

        public void setData(double[] d) { data = d; repaint(); }

        LineChartPanel() { setOpaque(false); setPreferredSize(new Dimension(0, 160)); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.length < 2) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight(), pad = 30;
            int chartW = w - pad*2, chartH = h - pad*2;
            double max = 1; for (double v : data) max = Math.max(max, v);

            int n = data.length;
            int[] xs = new int[n], ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = pad + (int)(i * chartW / (n - 1));
                ys[i] = pad + chartH - (int)(data[i] / max * chartH);
            }

            // fill under curve
            Polygon poly = new Polygon(xs, ys, n);
            poly.addPoint(xs[n-1], h - pad);
            poly.addPoint(xs[0], h - pad);
            g2.setColor(new Color(212, 175, 55, 30));
            g2.fillPolygon(poly);

            // line
            g2.setColor(LuxuryTheme.ACCENT_GOLD);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n-1; i++) g2.drawLine(xs[i], ys[i], xs[i+1], ys[i+1]);

            // dots
            g2.setColor(LuxuryTheme.ACCENT_GOLD);
            for (int i = 0; i < n; i++) {
                g2.fillOval(xs[i]-4, ys[i]-4, 8, 8);
            }

            g2.setColor(LuxuryTheme.BORDER_SUBTLE);
            g2.drawLine(pad, h-pad, w-pad, h-pad);
            g2.dispose();
        }
    }

    static class PieChartPanel extends JPanel {
        private String[] labels = {"A","B","C","D"};
        private int[] values = {30, 40, 15, 15};
        private Color[] colors = {LuxuryTheme.ACCENT_BLUE, LuxuryTheme.SUCCESS, LuxuryTheme.DANGER, LuxuryTheme.ACCENT_GOLD};
        private final List<SliceRegion> sliceRegions = new ArrayList<>();
        private int selectedIndex = -1;
        private int centerX;
        private int centerY;
        private int outerRadius;
        private int innerRadius;
        private int totalValue;

        public void setData(String[] l, int[] v, Color[] c) {
            labels = l;
            values = v;
            colors = c;
            selectedIndex = -1;
            repaint();
        }

        PieChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 160));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int idx = findSliceIndex(e.getX(), e.getY());
                    if (idx < 0) return;
                    selectedIndex = idx;
                    repaint();
                    int count = values[idx];
                    double pct = totalValue > 0 ? (count * 100.0 / totalValue) : 0.0;
                    JOptionPane.showMessageDialog(
                        PieChartPanel.this,
                        String.format("%s: %d order(s) (%.1f%%)", labels[idx], count, pct),
                        "Order Status",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int idx = findSliceIndex(e.getX(), e.getY());
                    if (idx >= 0) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        int count = values[idx];
                        double pct = totalValue > 0 ? (count * 100.0 / totalValue) : 0.0;
                        setToolTipText(String.format("%s: %d (%.1f%%)", labels[idx], count, pct));
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                        setToolTipText(null);
                    }
                }
            });
        }

        private int findSliceIndex(int x, int y) {
            int dx = x - centerX;
            int dy = y - centerY;
            int distSq = dx * dx + dy * dy;
            if (distSq > outerRadius * outerRadius || distSq < innerRadius * innerRadius) return -1;
            for (SliceRegion region : sliceRegions) {
                if (region.shape.contains(x, y)) return region.index;
            }
            return -1;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (values == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            totalValue = 0; for (int v : values) totalValue += v;
            if (totalValue == 0) { g2.dispose(); return; }

            int w = getWidth(), h = getHeight();
            int size = Math.min(w, h) - 30;
            int x = (w - size) / 2, y = (h - size) / 2;
            centerX = x + size / 2;
            centerY = y + size / 2;
            outerRadius = size / 2;
            innerRadius = size / 6;
            sliceRegions.clear();

            double startAngle = 90;
            for (int i = 0; i < values.length; i++) {
                double arc = 360.0 * values[i] / totalValue;
                Arc2D.Double slice = new Arc2D.Double(x, y, size, size, startAngle, arc, Arc2D.PIE);
                g2.setColor(i < colors.length ? colors[i] : LuxuryTheme.ACCENT_TEAL);
                g2.fill(slice);
                g2.setColor(LuxuryTheme.BG_CARD);
                g2.setStroke(new BasicStroke(2));
                g2.draw(slice);
                if (i == selectedIndex) {
                    g2.setColor(new Color(255, 255, 255, 210));
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.draw(slice);
                }
                sliceRegions.add(new SliceRegion(i, slice));
                startAngle += arc;
            }

            // donut hole
            g2.setColor(LuxuryTheme.BG_CARD);
            int hole = size / 3;
            g2.fillOval(x + size/2 - hole/2, y + size/2 - hole/2, hole, hole);
            innerRadius = hole / 2;

            g2.dispose();
        }

        private static class SliceRegion {
            final int index;
            final Shape shape;

            SliceRegion(int index, Shape shape) {
                this.index = index;
                this.shape = shape;
            }
        }
    }

    private static class LineBorder extends javax.swing.border.AbstractBorder {
        Color c; int t; boolean r;
        LineBorder(Color c, int t, boolean r) { this.c=c; this.t=t; this.r=r; }
        @Override public void paintBorder(Component comp, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c);
            if (r) g2.draw(new RoundRectangle2D.Double(x,y,w-1,h-1,12,12));
            else   g2.drawRect(x,y,w-1,h-1);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(t,t,t,t); }
    }

    private static class Bucket {
        int count;
        double revenue;
    }

    private static class TrendData {
        final String[] labels;
        final int[] orderCounts;
        final double[] revenueValues;

        TrendData(String[] labels, int[] orderCounts, double[] revenueValues) {
            this.labels = labels;
            this.orderCounts = orderCounts;
            this.revenueValues = revenueValues;
        }
    }

    private static class StatusData {
        final String[] labels;
        final int[] values;
        final Color[] colors;

        StatusData(String[] labels, int[] values, Color[] colors) {
            this.labels = labels;
            this.values = values;
            this.colors = colors;
        }
    }
}
