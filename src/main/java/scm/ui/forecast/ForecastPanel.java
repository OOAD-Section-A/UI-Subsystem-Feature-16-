package scm.ui.forecast;

import com.forecast.services.query.ForecastPointDto;
import com.forecast.services.query.ForecastQueryService;
import com.forecast.services.query.ForecastSeriesResponseDto;
import scm.ui.model.*;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * C-08 — Demand Forecasting & Reports
 *
 * Tabs:
 *   1. 📈 Forecasts          — CRUD on demand_forecasts (existing)
 *   2. 🔮 Time-Series        — NEW: ForecastQueryService (JAR) → forecast_timeseries
 *                               Shows both a Java2D confidence-band chart AND a JTable
 *   3. 🔔 Reorder Suggestions— Low-stock + forecast reorder signals (existing)
 *   4. 📋 Reports            — Run-time report generator (existing)
 *
 * Demand Forecasting Subsystem JAR integration:
 *   - Uses com.forecast.services.query.ForecastQueryService
 *   - Connects to the UI system DB: jdbc:mysql://localhost:3306/OOAD
 *   - Reads from forecast_timeseries (graph-ready time-series data)
 *   - Falls back gracefully if the forecasting DB is unreachable
 */
public class ForecastPanel extends JPanel {

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();

    // ── Forecast JAR DB config (aligned with updated integration guide) ───────
    private static final String DEFAULT_DB_URL =
        "jdbc:mysql://localhost:3306/OOAD?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASS = "tarun12345";
    private static final DbConfig DB_CONFIG = loadDbConfig();
    private static volatile ForecastQueryService forecastQueryService;

    public ForecastPanel() {
        setLayout(new BorderLayout());
        setBackground(LuxuryTheme.BG_PANEL);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LuxuryTheme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 0, 28));
        header.add(LuxuryTheme.sectionTitle("Demand Forecasting  &  Reports"), BorderLayout.WEST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(LuxuryTheme.BG_PANEL);
        tabs.setFont(LuxuryTheme.FONT_SUBHEAD);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        tabs.addTab("📈 Forecasts",           new ForecastCrudPanel());
        tabs.addTab("🔮 Time-Series",         new TimeSeriesPanel());       // ← NEW
        tabs.addTab("🧩 JAR Integration",     new IntegrationPanel());
        tabs.addTab("🔔 Reorder Suggestions", new ReorderSuggestionsPanel());
        tabs.addTab("📋 Reports",             new ReportsPanel());

        add(header, BorderLayout.NORTH);
        add(tabs,   BorderLayout.CENTER);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Forecast CRUD (unchanged)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class ForecastCrudPanel extends CrudPanel {
        ForecastCrudPanel() {
            init("Demand Forecasts", new String[]{
                "Forecast ID","Product ID","Period Start","Period End",
                "Algorithm","Forecasted Qty","Confidence %","Reorder Suggested","Created By"
            });
        }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            List<DemandForecast> list = facade.getAllForecasts();
            SwingUtilities.invokeLater(() -> {
                for (DemandForecast f : list) {
                    tableModel.addRow(new Object[]{
                        f.getForecastId(), f.getProductId(),
                        f.getForecastPeriodStart(), f.getForecastPeriodEnd(),
                        f.getForecastAlgorithm(), f.getForecastedQty(),
                        AppUtils.percent(f.getConfidenceScore()),
                        f.isReorderSuggested() ? "Yes" : "No",
                        f.getCreatedBy()
                    });
                }
                updateRecordCount();
            });
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Generate Demand Forecast", 560, 500);
            JTextField idF    = LuxuryTheme.textField(AppUtils.newId("FC"));
            JTextField pidF   = LuxuryTheme.textField("PROD-001");
            JTextField startF = LuxuryTheme.textField("2024-01-01");
            JTextField endF   = LuxuryTheme.textField("2024-03-31");
            JComboBox<String> algCB = LuxuryTheme.comboBox(new String[]{
                "MOVING_AVERAGE","EXPONENTIAL_SMOOTHING","ARIMA","ML_REGRESSION","SEASONAL_DECOMP"
            });
            JSpinner qtySp   = LuxuryTheme.spinner(1, 999999, 100);
            JSpinner confSp  = new JSpinner(new SpinnerNumberModel(75.0, 0.0, 100.0, 0.1));
            JCheckBox reoChk = new JCheckBox("Reorder Suggested");
            reoChk.setOpaque(false); reoChk.setForeground(LuxuryTheme.TEXT_PRIMARY);
            JTextField createdByF = LuxuryTheme.textField(
                facade.getCurrentUser() != null ? facade.getCurrentUser().getUsername() : "SYSTEM"
            );

            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Forecast Details")
                .addField("Forecast ID *",        idF)
                .addField("Product ID *",         pidF)
                .addField("Period Start *",        startF)
                .addField("Period End *",          endF)
                .addField("Algorithm *",           algCB)
                .addField("Forecasted Quantity *", qtySp)
                .addField("Confidence Score %",    confSp)
                .addField("Reorder Suggestion",    reoChk)
                .addField("Created By",            createdByF)
                .build();

            addDialogButtons(d, form, () -> {
                DemandForecast f = new DemandForecast();
                f.setForecastId(idF.getText().trim());
                f.setProductId(pidF.getText().trim());
                f.setForecastPeriodStart(startF.getText().trim());
                f.setForecastPeriodEnd(endF.getText().trim());
                f.setForecastAlgorithm((String) algCB.getSelectedItem());
                f.setForecastedQty((int) qtySp.getValue());
                f.setConfidenceScore((double) confSp.getValue());
                f.setReorderSuggested(reoChk.isSelected());
                f.setCreatedBy(createdByF.getText().trim());
                int r = facade.createForecast(f);
                if (r > 0) { d.dispose(); loadDataAsync(); AppUtils.showSuccess(this, "Forecast created."); }
                else AppUtils.showError(d, "Failed to create forecast.");
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            DemandForecast f = facade.getAllForecasts().stream()
                .filter(fc -> fc.getForecastId().equals(id)).findFirst().orElse(null);
            if (f == null) return;

            JDialog d = createDialog("Edit Forecast: " + id, 500, 380);
            JSpinner qtySp  = LuxuryTheme.spinner(1, 999999, f.getForecastedQty());
            JSpinner confSp = new JSpinner(new SpinnerNumberModel(f.getConfidenceScore(), 0.0, 100.0, 0.1));
            JCheckBox reoChk = new JCheckBox("Reorder Suggested", f.isReorderSuggested());
            reoChk.setOpaque(false); reoChk.setForeground(LuxuryTheme.TEXT_PRIMARY);
            JComboBox<String> algCB = LuxuryTheme.comboBox(new String[]{
                "MOVING_AVERAGE","EXPONENTIAL_SMOOTHING","ARIMA","ML_REGRESSION","SEASONAL_DECOMP"
            });
            algCB.setSelectedItem(f.getForecastAlgorithm());
            JTextField endF = LuxuryTheme.textField(f.getForecastPeriodEnd());

            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Edit Forecast: " + id)
                .addField("Algorithm",          algCB)
                .addField("Forecasted Qty",     qtySp)
                .addField("Confidence Score %", confSp)
                .addField("Period End",         endF)
                .addField("Reorder Suggestion", reoChk)
                .build();

            addDialogButtons(d, form, () -> {
                f.setForecastAlgorithm((String) algCB.getSelectedItem());
                f.setForecastedQty((int) qtySp.getValue());
                f.setConfidenceScore((double) confSp.getValue());
                f.setForecastPeriodEnd(endF.getText().trim());
                f.setReorderSuggested(reoChk.isSelected());
                facade.updateForecast(f);
                d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) {
            facade.deleteForecast((String) tableModel.getValueAt(row, 0));
            loadDataAsync();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 🔮 Time-Series Panel — NEW: Uses ForecastQueryService from JAR
    //
    // Connects to forecast_timeseries via ForecastQueryService.
    // Shows:
    //   (top)    Java2D chart with forecast line + confidence band
    //   (bottom) JTable with Month | Forecast | Lower | Upper
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class TimeSeriesPanel extends JPanel {

        private List<ForecastPointDto> seriesData = new ArrayList<>();
        private JTextField productIdField;
        private JLabel statusLabel;
        private DefaultTableModel seriesTableModel;
        private TimeSeriesChart chartPanel;

        TimeSeriesPanel() {
            setLayout(new BorderLayout(0, 0));
            setBackground(LuxuryTheme.BG_PANEL);

            // ── Controls bar ──────────────────────────────────────────────
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            controls.setBackground(LuxuryTheme.BG_PANEL);
            controls.setBorder(BorderFactory.createEmptyBorder(10, 28, 0, 28));

            controls.add(LuxuryTheme.mutedLabel("Product ID:"));
            productIdField = LuxuryTheme.textField("P1001");
            productIdField.setPreferredSize(new Dimension(160, 34));
            controls.add(productIdField);

            JButton fetchBtn  = LuxuryTheme.goldButton("🔮 Fetch Time-Series");
            JButton exportBtn = LuxuryTheme.ghostButton("↓ Export CSV");
            controls.add(fetchBtn);
            controls.add(exportBtn);

            statusLabel = LuxuryTheme.mutedLabel("Enter a Product ID and click Fetch Time-Series.");
            controls.add(statusLabel);

            fetchBtn.addActionListener(e -> fetchTimeSeries());
            productIdField.addActionListener(e -> fetchTimeSeries());
            exportBtn.addActionListener(e -> exportTable());

            // ── Chart card ────────────────────────────────────────────────
            chartPanel = new TimeSeriesChart();
            JPanel chartCard = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(LuxuryTheme.BG_CARD);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            chartCard.setOpaque(false);
            chartCard.setBorder(BorderFactory.createEmptyBorder(16, 28, 12, 28));
            chartCard.add(chartPanel, BorderLayout.CENTER);
            chartPanel.setPreferredSize(new Dimension(600, 280));

            // ── Table header ──────────────────────────────────────────────
            JPanel tableHeader = new JPanel(new BorderLayout());
            tableHeader.setBackground(LuxuryTheme.BG_PANEL);
            tableHeader.setBorder(BorderFactory.createEmptyBorder(6, 28, 4, 28));
            tableHeader.add(LuxuryTheme.subHeading("Forecast Time-Series Data"), BorderLayout.WEST);

            // ── JTable ────────────────────────────────────────────────────
            seriesTableModel = AppUtils.tableModel(
                new String[]{"Month (Index)", "Forecast Value", "Lower Bound", "Upper Bound"});
            JTable seriesTable = new JTable(seriesTableModel);
            seriesTable.setAutoCreateRowSorter(true);
            JScrollPane sp = LuxuryTheme.styledTable(seriesTable);
            LuxuryTheme.styleScrollBar(sp.getVerticalScrollBar());
            sp.setPreferredSize(new Dimension(600, 160));

            JPanel tableWrap = new JPanel(new BorderLayout());
            tableWrap.setBackground(LuxuryTheme.BG_PANEL);
            tableWrap.setBorder(BorderFactory.createEmptyBorder(0, 28, 16, 28));
            tableWrap.add(sp, BorderLayout.CENTER);

            // ── Info banner ───────────────────────────────────────────────
            JPanel infoBanner = buildInfoBanner();

            // ── Bottom section (table header + table) ─────────────────────
            JPanel bottomSection = new JPanel(new BorderLayout());
            bottomSection.setBackground(LuxuryTheme.BG_PANEL);
            bottomSection.add(tableHeader, BorderLayout.NORTH);
            bottomSection.add(tableWrap,   BorderLayout.CENTER);

            // ── Centre split: chart + table ───────────────────────────────
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartCard, bottomSection);
            splitPane.setDividerLocation(310);
            splitPane.setResizeWeight(0.6);
            splitPane.setOpaque(false);
            splitPane.setBackground(LuxuryTheme.BG_PANEL);
            splitPane.setBorder(null);
            splitPane.setDividerSize(4);

            add(controls,    BorderLayout.NORTH);
            add(splitPane,   BorderLayout.CENTER);
            add(infoBanner,  BorderLayout.SOUTH);
        }

        /** Gold info banner explaining the data source */
        private JPanel buildInfoBanner() {
            JPanel banner = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
            banner.setBackground(new Color(40, 35, 10));
            banner.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LuxuryTheme.ACCENT_GOLD));
            JLabel ico = new JLabel("ℹ");
            ico.setFont(new Font("Segoe UI", Font.BOLD, 14));
            ico.setForeground(LuxuryTheme.ACCENT_GOLD);
            JLabel txt = LuxuryTheme.mutedLabel(
                "Data source: Updated Demand Forecasting JAR  ·  API: ForecastQueryService  ·  Table: forecast_timeseries");
            txt.setForeground(new Color(200, 180, 100));
            banner.add(ico);
            banner.add(txt);
            return banner;
        }

        /** Fetch time-series data using ForecastQueryService from the JAR */
        private void fetchTimeSeries() {
            String productId = productIdField.getText() == null ? "" : productIdField.getText().trim();
            if (productId.isEmpty()) {
                AppUtils.showError(this, "Please enter a Product ID.");
                return;
            }

            statusLabel.setText("Fetching from UI system DB…");
            seriesTableModel.setRowCount(0);
            seriesData.clear();
            chartPanel.repaint();

            SwingWorker<ForecastSeriesResponseDto, Void> worker = new SwingWorker<>() {
                @Override
                protected ForecastSeriesResponseDto doInBackground() throws Exception {
                    ForecastQueryService service = getForecastQueryService();
                    return service.getLatestForecastSeries(productId);
                }

                @Override
                protected void done() {
                    try {
                        ForecastSeriesResponseDto response = get();
                        List<ForecastPointDto> series = response.getSeries();

                        if (series == null || series.isEmpty()) {
                            statusLabel.setText("No time-series data found for product: " + productId
                                + "  (forecast_timeseries table may be empty for this product)");
                            seriesData.clear();
                            chartPanel.repaint();
                            return;
                        }

                        seriesData = series;

                        // Populate table
                        seriesTableModel.setRowCount(0);
                        for (ForecastPointDto p : series) {
                            seriesTableModel.addRow(new Object[]{
                                "Month " + p.getTimeIndex(),
                                formatDecimal(p.getForecastValue()),
                                formatDecimal(p.getLowerBound()),
                                formatDecimal(p.getUpperBound())
                            });
                        }

                        statusLabel.setText(
                            "Loaded " + series.size() + " point(s) for product: " + productId
                            + "  ·  Forecast ID: " + (response.getForecastId() != null ? response.getForecastId() : "—")
                        );
                        chartPanel.repaint();

                    } catch (Exception ex) {
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        statusLabel.setText("Error: " + msg);
                        AppUtils.showError(TimeSeriesPanel.this,
                            "Could not fetch from UI system DB.\n\n"
                            + "Check that:\n"
                            + "  • OOAD DB is running\n"
                            + "  • forecast_timeseries table exists\n"
                            + "  • The JAR is on the classpath\n\n"
                            + "Details: " + msg);
                    }
                }
            };
            worker.execute();
        }

        private String formatDecimal(BigDecimal bd) {
            return bd != null ? String.format("%.2f", bd) : "—";
        }

        private void exportTable() {
            if (seriesData.isEmpty()) {
                AppUtils.showError(this, "No data to export. Fetch a product first.");
                return;
            }
            DefaultTableModel tm = AppUtils.tableModel(
                new String[]{"Month (Index)", "Forecast Value", "Lower Bound", "Upper Bound"});
            for (ForecastPointDto p : seriesData) {
                tm.addRow(new Object[]{
                    "Month " + p.getTimeIndex(),
                    formatDecimal(p.getForecastValue()),
                    formatDecimal(p.getLowerBound()),
                    formatDecimal(p.getUpperBound())
                });
            }
            new AppUtils.CsvExportStrategy().export(new JTable(tm), this);
        }

        // ── Java2D chart: forecast line + confidence band ──────────────────
        class TimeSeriesChart extends JPanel {

            TimeSeriesChart() {
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,  RenderingHints.VALUE_STROKE_PURE);

                int w = getWidth(), h = getHeight();
                int padL = 64, padR = 30, padT = 24, padB = 50;
                int chartW = w - padL - padR;
                int chartH = h - padT - padB;

                // Empty state
                if (seriesData == null || seriesData.isEmpty()) {
                    g2.setColor(LuxuryTheme.TEXT_MUTED);
                    g2.setFont(LuxuryTheme.FONT_BODY);
                    g2.drawString("Enter a Product ID and click Fetch Time-Series to display the chart.", padL + 10, h / 2);
                    g2.dispose();
                    return;
                }

                int n = seriesData.size();

                // Determine Y range across all three series
                double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
                for (ForecastPointDto p : seriesData) {
                    if (p.getForecastValue() != null) {
                        minY = Math.min(minY, p.getForecastValue().doubleValue());
                        maxY = Math.max(maxY, p.getForecastValue().doubleValue());
                    }
                    if (p.getLowerBound() != null)  minY = Math.min(minY, p.getLowerBound().doubleValue());
                    if (p.getUpperBound() != null)  maxY = Math.max(maxY, p.getUpperBound().doubleValue());
                }
                if (maxY == minY) { minY -= 1; maxY += 1; }
                double yRange = maxY - minY;

                // Helper lambdas
                final double fMinY = minY, fMaxY = maxY;
                java.util.function.IntUnaryOperator xOf = i ->
                    padL + (n > 1 ? (int) ((double) i / (n - 1) * chartW) : chartW / 2);
                java.util.function.DoubleUnaryOperator yOf = val ->
                    padT + (int) ((1.0 - (val - fMinY) / (fMaxY - fMinY)) * chartH);

                // Grid lines
                g2.setStroke(new BasicStroke(0.8f));
                int gridCount = 5;
                g2.setFont(LuxuryTheme.FONT_SMALL);
                for (int i = 0; i <= gridCount; i++) {
                    double val = minY + (double) i / gridCount * yRange;
                    int y = (int) yOf.applyAsDouble(val);
                    g2.setColor(LuxuryTheme.BORDER_SUBTLE);
                    g2.drawLine(padL, y, padL + chartW, y);
                    g2.setColor(LuxuryTheme.TEXT_MUTED);
                    g2.drawString(String.format("%.0f", val), 4, y + 4);
                }

                // ── Confidence band (filled area between lower and upper) ──
                if (seriesData.get(0).getLowerBound() != null && seriesData.get(0).getUpperBound() != null) {
                    int[] bandXPoints = new int[n * 2];
                    int[] bandYPoints = new int[n * 2];
                    for (int i = 0; i < n; i++) {
                        ForecastPointDto p = seriesData.get(i);
                        bandXPoints[i]         = xOf.applyAsInt(i);
                        bandYPoints[i]         = (int) yOf.applyAsDouble(
                            p.getUpperBound() != null ? p.getUpperBound().doubleValue() : p.getForecastValue().doubleValue());
                        bandXPoints[n * 2 - 1 - i] = xOf.applyAsInt(i);
                        bandYPoints[n * 2 - 1 - i] = (int) yOf.applyAsDouble(
                            p.getLowerBound() != null ? p.getLowerBound().doubleValue() : p.getForecastValue().doubleValue());
                    }
                    g2.setColor(new Color(80, 200, 180, 35));   // translucent teal band
                    g2.fillPolygon(bandXPoints, bandYPoints, n * 2);

                    // Upper bound line (dashed, muted)
                    g2.setColor(new Color(80, 200, 180, 120));
                    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                        0, new float[]{4, 4}, 0));
                    for (int i = 0; i < n - 1; i++) {
                        g2.drawLine(xOf.applyAsInt(i),
                            (int) yOf.applyAsDouble(
                                seriesData.get(i).getUpperBound() != null
                                    ? seriesData.get(i).getUpperBound().doubleValue()
                                    : seriesData.get(i).getForecastValue().doubleValue()),
                            xOf.applyAsInt(i + 1),
                            (int) yOf.applyAsDouble(
                                seriesData.get(i + 1).getUpperBound() != null
                                    ? seriesData.get(i + 1).getUpperBound().doubleValue()
                                    : seriesData.get(i + 1).getForecastValue().doubleValue()));
                    }

                    // Lower bound line (dashed, muted)
                    g2.setColor(new Color(200, 150, 60, 120));
                    for (int i = 0; i < n - 1; i++) {
                        g2.drawLine(xOf.applyAsInt(i),
                            (int) yOf.applyAsDouble(
                                seriesData.get(i).getLowerBound() != null
                                    ? seriesData.get(i).getLowerBound().doubleValue()
                                    : seriesData.get(i).getForecastValue().doubleValue()),
                            xOf.applyAsInt(i + 1),
                            (int) yOf.applyAsDouble(
                                seriesData.get(i + 1).getLowerBound() != null
                                    ? seriesData.get(i + 1).getLowerBound().doubleValue()
                                    : seriesData.get(i + 1).getForecastValue().doubleValue()));
                    }
                }

                // ── Main forecast line (solid, teal) ──────────────────────
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(LuxuryTheme.ACCENT_TEAL);
                for (int i = 0; i < n - 1; i++) {
                    BigDecimal v1 = seriesData.get(i).getForecastValue();
                    BigDecimal v2 = seriesData.get(i + 1).getForecastValue();
                    if (v1 == null || v2 == null) continue;
                    g2.drawLine(
                        xOf.applyAsInt(i),     (int) yOf.applyAsDouble(v1.doubleValue()),
                        xOf.applyAsInt(i + 1), (int) yOf.applyAsDouble(v2.doubleValue())
                    );
                }

                // Dots + X-axis labels
                for (int i = 0; i < n; i++) {
                    ForecastPointDto p = seriesData.get(i);
                    if (p.getForecastValue() == null) continue;
                    int x = xOf.applyAsInt(i);
                    int y = (int) yOf.applyAsDouble(p.getForecastValue().doubleValue());

                    // Dot
                    g2.setColor(LuxuryTheme.BG_CARD);
                    g2.fillOval(x - 5, y - 5, 10, 10);
                    g2.setColor(LuxuryTheme.ACCENT_TEAL);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(x - 5, y - 5, 10, 10);

                    // X-axis label
                    g2.setFont(LuxuryTheme.FONT_SMALL);
                    g2.setColor(LuxuryTheme.TEXT_MUTED);
                    String label = "M" + p.getTimeIndex();
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(label, x - fm.stringWidth(label) / 2, padT + chartH + 16);
                }

                // Axes
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(LuxuryTheme.TEXT_MUTED);
                g2.drawLine(padL, padT, padL, padT + chartH);
                g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

                // Y-axis title
                g2.setFont(LuxuryTheme.FONT_SMALL);
                g2.setColor(LuxuryTheme.TEXT_MUTED);
                Graphics2D gRot = (Graphics2D) g.create();
                gRot.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gRot.rotate(-Math.PI / 2, 12, h / 2);
                gRot.setFont(LuxuryTheme.FONT_SMALL);
                gRot.setColor(LuxuryTheme.TEXT_MUTED);
                gRot.drawString("Demand Qty", 12 - 30, h / 2);
                gRot.dispose();

                // Legend
                int legX = padL;
                int legY = h - 14;
                // Forecast line
                g2.setColor(LuxuryTheme.ACCENT_TEAL);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(legX, legY, legX + 20, legY);
                g2.fillOval(legX + 6, legY - 4, 8, 8);
                g2.setFont(LuxuryTheme.FONT_SMALL);
                g2.setColor(LuxuryTheme.TEXT_SECOND);
                g2.drawString("Forecast", legX + 26, legY + 4);
                // Upper bound
                g2.setColor(new Color(80, 200, 180, 180));
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
                g2.drawLine(legX + 100, legY, legX + 120, legY);
                g2.setColor(LuxuryTheme.TEXT_SECOND);
                g2.drawString("Upper Bound", legX + 126, legY + 4);
                // Lower bound
                g2.setColor(new Color(200, 150, 60, 180));
                g2.drawLine(legX + 225, legY, legX + 245, legY);
                g2.setColor(LuxuryTheme.TEXT_SECOND);
                g2.drawString("Lower Bound", legX + 251, legY + 4);
                // Band
                g2.setColor(new Color(80, 200, 180, 80));
                g2.fillRect(legX + 350, legY - 6, 20, 12);
                g2.setColor(LuxuryTheme.TEXT_SECOND);
                g2.drawString("Confidence Band", legX + 376, legY + 4);

                g2.dispose();
            }
        }
    }

    class IntegrationPanel extends JPanel {
        private final JTextField productIdField;
        private final JTextArea output;

        IntegrationPanel() {
            setLayout(new BorderLayout(0, 10));
            setBackground(LuxuryTheme.BG_PANEL);

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            controls.setBackground(LuxuryTheme.BG_PANEL);
            controls.setBorder(BorderFactory.createEmptyBorder(14, 28, 0, 28));

            controls.add(LuxuryTheme.mutedLabel("Product ID Probe:"));
            productIdField = LuxuryTheme.textField("P1001");
            productIdField.setPreferredSize(new Dimension(180, 34));
            JButton verifyBtn = LuxuryTheme.goldButton("✓ Validate Forecast JAR");
            verifyBtn.addActionListener(e -> runIntegrationProbe());
            controls.add(productIdField);
            controls.add(verifyBtn);

            output = new JTextArea();
            output.setEditable(false);
            output.setLineWrap(true);
            output.setWrapStyleWord(true);
            output.setFont(LuxuryTheme.FONT_BODY);
            output.setBackground(LuxuryTheme.BG_CARD);
            output.setForeground(LuxuryTheme.TEXT_PRIMARY);
            output.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
            output.setText(
                "Forecasting JAR Integration Ready\n\n"
                    + "JAR API:\n"
                    + "  com.forecast.services.query.ForecastQueryService\n\n"
                    + "Resolved DB Configuration:\n"
                    + "  URL      = " + DB_CONFIG.url + "\n"
                    + "  Username = " + DB_CONFIG.user + "\n"
                    + "  Password = ********\n\n"
                    + "Click \"Validate Forecast JAR\" to run a live query probe."
            );

            JScrollPane sp = new JScrollPane(output);
            sp.setBorder(BorderFactory.createEmptyBorder(0, 28, 16, 28));
            LuxuryTheme.styleScrollBar(sp.getVerticalScrollBar());

            add(controls, BorderLayout.NORTH);
            add(sp, BorderLayout.CENTER);
        }

        private void runIntegrationProbe() {
            String productId = productIdField.getText() == null ? "" : productIdField.getText().trim();
            if (productId.isEmpty()) {
                AppUtils.showError(this, "Please enter a Product ID for probe.");
                return;
            }

            output.setText("Running live integration probe...");
            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() throws Exception {
                    ForecastQueryService service = getForecastQueryService();
                    ForecastSeriesResponseDto response = service.getLatestForecastSeries(productId);
                    int points = response.getSeries() == null ? 0 : response.getSeries().size();
                    return "Integration probe succeeded.\n\n"
                        + "Product ID: " + productId + "\n"
                        + "Forecast ID: " + (response.getForecastId() == null ? "null" : response.getForecastId()) + "\n"
                        + "Points Returned: " + points + "\n\n"
                        + "Interpretation:\n"
                        + (points == 0
                            ? "JAR + DB connectivity is working, but no forecast_timeseries rows exist yet for this product."
                            : "JAR + DB connectivity + query path are fully operational.");
                }

                @Override
                protected void done() {
                    try {
                        output.setText(get());
                    } catch (Exception ex) {
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        output.setText(
                            "Integration probe failed.\n\n"
                                + "Reason: " + msg + "\n\n"
                                + "Check DB credentials, OOAD reachability, and forecast_timeseries availability."
                        );
                    }
                }
            };
            worker.execute();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Reorder Suggestions (unchanged)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class ReorderSuggestionsPanel extends JPanel {
        private DefaultTableModel model;
        private JTable table;

        ReorderSuggestionsPanel() {
            setLayout(new BorderLayout(0, 10));
            setBackground(LuxuryTheme.BG_PANEL);

            JPanel top = new JPanel(new BorderLayout(16, 0));
            top.setBackground(LuxuryTheme.BG_PANEL);
            top.setBorder(BorderFactory.createEmptyBorder(16, 28, 0, 28));
            top.add(LuxuryTheme.subHeading("Products Requiring Reorder"), BorderLayout.WEST);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnRow.setOpaque(false);
            JButton refreshBtn = LuxuryTheme.primaryButton("↻ Refresh");
            JButton exportBtn  = LuxuryTheme.ghostButton("↓ Export CSV");
            refreshBtn.addActionListener(e -> loadSuggestions());
            exportBtn.addActionListener(e -> new AppUtils.CsvExportStrategy().export(table, this));
            btnRow.add(refreshBtn); btnRow.add(exportBtn);
            top.add(btnRow, BorderLayout.EAST);

            model = AppUtils.tableModel(new String[]{
                "Product ID","Current Stock","Reorder Threshold","Reorder Qty","Forecast Qty","Status"
            });
            table = new JTable(model);
            JScrollPane sp = LuxuryTheme.styledTable(table);
            LuxuryTheme.styleScrollBar(sp.getVerticalScrollBar());

            JPanel tableWrap = new JPanel(new BorderLayout());
            tableWrap.setBackground(LuxuryTheme.BG_PANEL);
            tableWrap.setBorder(BorderFactory.createEmptyBorder(10, 28, 16, 28));
            tableWrap.add(sp, BorderLayout.CENTER);

            add(top,       BorderLayout.NORTH);
            add(tableWrap, BorderLayout.CENTER);

            loadSuggestions();
        }

        private void loadSuggestions() {
            SwingWorker<Void, Void> w = new SwingWorker<>() {
                @Override protected Void doInBackground() {
                    List<StockLevel> lowStock = facade.getLowStockItems();
                    List<DemandForecast> forecasts = facade.getAllForecasts();
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (StockLevel sl : lowStock) {
                            int forecastQty = forecasts.stream()
                                .filter(f -> f.getProductId().equals(sl.getProductId()) && f.isReorderSuggested())
                                .mapToInt(DemandForecast::getForecastedQty).sum();
                            String status = sl.getCurrentStockQty() == 0 ? "🔴 OUT OF STOCK"
                                : sl.getCurrentStockQty() < sl.getSafetyStockLevel() ? "🟠 CRITICAL"
                                : "🟡 LOW";
                            model.addRow(new Object[]{
                                sl.getProductId(), sl.getCurrentStockQty(),
                                sl.getReorderThreshold(), sl.getReorderQuantity(),
                                forecastQty > 0 ? forecastQty : "—", status
                            });
                        }
                    });
                    return null;
                }
            };
            w.execute();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Reports Panel (unchanged)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    class ReportsPanel extends JPanel {
        private DefaultTableModel model;
        private JTable table;
        private JComboBox<String> reportTypeCB;
        private JLabel reportStatusLabel;
        private JButton runBtn;

        ReportsPanel() {
            setLayout(new BorderLayout(0, 10));
            setBackground(LuxuryTheme.BG_PANEL);

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            controls.setBackground(LuxuryTheme.BG_PANEL);
            controls.setBorder(BorderFactory.createEmptyBorder(10, 28, 0, 28));
            controls.add(LuxuryTheme.mutedLabel("Report Type:"));
            reportTypeCB = LuxuryTheme.comboBox(new String[]{
                "Forecast Summary","Low Stock Report","Order Performance","Shipment Status"
            });
            reportTypeCB.setPreferredSize(new Dimension(220, 34));
            runBtn = LuxuryTheme.goldButton("▶  Run Report");
            JButton expBtn = LuxuryTheme.ghostButton("↓ Export CSV");
            runBtn.addActionListener(e -> runReport());
            expBtn.addActionListener(e -> new AppUtils.CsvExportStrategy().export(table, this));
            controls.add(reportTypeCB); controls.add(runBtn); controls.add(expBtn);
            reportStatusLabel = LuxuryTheme.mutedLabel("Select report type and click Run Report.");
            controls.add(reportStatusLabel);

            model = AppUtils.tableModel(new String[]{"—"});
            table = new JTable(model);
            JScrollPane sp = LuxuryTheme.styledTable(table);
            LuxuryTheme.styleScrollBar(sp.getVerticalScrollBar());

            JPanel tableWrap = new JPanel(new BorderLayout());
            tableWrap.setBackground(LuxuryTheme.BG_PANEL);
            tableWrap.setBorder(BorderFactory.createEmptyBorder(10, 28, 16, 28));
            tableWrap.add(sp, BorderLayout.CENTER);

            add(controls,  BorderLayout.NORTH);
            add(tableWrap, BorderLayout.CENTER);
        }

        private void runReport() {
            String type = (String) reportTypeCB.getSelectedItem();
            runBtn.setEnabled(false);
            reportStatusLabel.setText("Generating report…");
            SwingWorker<ReportResult, Void> w = new SwingWorker<>() {
                @Override protected ReportResult doInBackground() {
                    ReportResult result = new ReportResult();
                    switch (type) {
                        case "Forecast Summary":
                            result.columns = new String[]{"Forecast ID","Product ID","Period Start","Period End","Forecasted Qty","Confidence %","Reorder"};
                            for (DemandForecast f : facade.getAllForecasts())
                                result.rows.add(new Object[]{
                                    f.getForecastId(), f.getProductId(),
                                    f.getForecastPeriodStart(), f.getForecastPeriodEnd(),
                                    f.getForecastedQty(), AppUtils.percent(f.getConfidenceScore()),
                                    f.isReorderSuggested() ? "Yes" : "No"
                                });
                            break;
                        case "Low Stock Report":
                            result.columns = new String[]{"Product ID","Current Qty","Threshold","Safety Stock","Available Qty"};
                            for (StockLevel s : facade.getLowStockItems())
                                result.rows.add(new Object[]{
                                    s.getProductId(), s.getCurrentStockQty(),
                                    s.getReorderThreshold(), s.getSafetyStockLevel(),
                                    s.getAvailableStockQty()
                                });
                            break;
                        case "Order Performance":
                            result.columns = new String[]{"Order ID","Customer","Status","Total (₹)","Payment","Channel","Date"};
                            for (Order o : facade.getAllOrders())
                                result.rows.add(new Object[]{
                                    o.getOrderId(), o.getCustomerId(), o.getOrderStatus(),
                                    AppUtils.currency(o.getTotalAmount()), o.getPaymentStatus(),
                                    o.getSalesChannel(), o.getOrderDate()
                                });
                            break;
                        case "Shipment Status":
                            result.columns = new String[]{"Shipment ID","Order ID","Status","Carrier","Tracking","Priority","Cost (₹)"};
                            for (Shipment s : facade.getAllShipments())
                                result.rows.add(new Object[]{
                                    s.getShipmentId(), s.getOrderId(), s.getShipmentStatus(),
                                    s.getCarrierId(), s.getTrackingId(), s.getShippingPriority(),
                                    AppUtils.currency(s.getCalculatedCost())
                                });
                            break;
                        default:
                            result.columns = new String[]{"Info"};
                    }
                    return result;
                }
                @Override protected void done() {
                    try {
                        ReportResult result = get();
                        DefaultTableModel newModel = AppUtils.tableModel(result.columns);
                        for (Object[] row : result.rows) newModel.addRow(row);
                        table.setModel(newModel);
                        model = newModel;
                        table.setAutoCreateRowSorter(true);
                        table.revalidate(); table.repaint();
                        reportStatusLabel.setText(result.rows.isEmpty()
                            ? "No records found for " + type + "."
                            : "Loaded " + result.rows.size() + " row(s) for " + type + ".");
                    } catch (Exception ex) {
                        reportStatusLabel.setText("Failed to generate report.");
                    } finally {
                        runBtn.setEnabled(true);
                    }
                }
            };
            w.execute();
        }

        private static class ReportResult {
            String[] columns = {};
            List<Object[]> rows = new ArrayList<>();
        }
    }

    private static ForecastQueryService getForecastQueryService() {
        if (forecastQueryService == null) {
            synchronized (ForecastPanel.class) {
                if (forecastQueryService == null) {
                    forecastQueryService = new ForecastQueryService(DB_CONFIG.url, DB_CONFIG.user, DB_CONFIG.pass);
                }
            }
        }
        return forecastQueryService;
    }

    private static DbConfig loadDbConfig() {
        Properties props = new Properties();
        try (InputStream cp = ForecastPanel.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (cp != null) props.load(cp);
        } catch (IOException e) {
            System.err.println("[Forecast] Could not read classpath database.properties: " + e.getMessage());
        }
        Path localFile = Path.of("database.properties");
        if (Files.exists(localFile)) {
            try (InputStream file = Files.newInputStream(localFile)) {
                Properties localProps = new Properties();
                localProps.load(file);
                props.putAll(localProps);
            } catch (IOException e) {
                System.err.println("[Forecast] Could not read local database.properties: " + e.getMessage());
            }
        }

        String url = firstNonBlank(
            System.getProperty("db.url"),
            System.getenv("DB_URL"),
            System.getenv("SCM_DB_URL"),
            props.getProperty("db.url"),
            DEFAULT_DB_URL
        );
        String user = firstNonBlank(
            System.getProperty("db.username"),
            System.getenv("DB_USERNAME"),
            System.getenv("SCM_DB_USER"),
            props.getProperty("db.username"),
            props.getProperty("db.user"),
            DEFAULT_DB_USER
        );
        String pass = firstNonBlank(
            System.getProperty("db.password"),
            System.getenv("DB_PASSWORD"),
            System.getenv("SCM_DB_PASSWORD"),
            props.getProperty("db.password"),
            DEFAULT_DB_PASS
        );
        return new DbConfig(url, user, pass);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private record DbConfig(String url, String user, String pass) {}
}
