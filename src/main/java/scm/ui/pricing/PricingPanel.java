package scm.ui.pricing;

import scm.ui.model.*;
import scm.ui.patterns.SupplyChainFacade;
import scm.ui.util.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * C-07 — Pricing, Discount & Commission UI
 * Tabs: Price List | Promotions | Discount Policies | Contracts | Approvals | Tiers | Segments | Commissions
 */
public class PricingPanel extends JPanel {

    private final SupplyChainFacade facade = SupplyChainFacade.getInstance();

    public PricingPanel() {
        setLayout(new BorderLayout());
        setBackground(LuxuryTheme.BG_PANEL);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LuxuryTheme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(24, 28, 0, 28));
        header.add(LuxuryTheme.sectionTitle("Pricing, Discount  &  Commission"), BorderLayout.WEST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(LuxuryTheme.BG_PANEL);
        tabs.setFont(LuxuryTheme.FONT_SUBHEAD);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        tabs.addTab("💰 Price List",       new PriceListCrudPanel());
        tabs.addTab("🎟  Promotions",       new PromotionCrudPanel());
        tabs.addTab("📋 Discount Policies", new DiscountPolicyCrudPanel());
        tabs.addTab("📄 Contracts",         new ContractCrudPanel());
        tabs.addTab("✅ Approvals",         new ApprovalCrudPanel());
        tabs.addTab("🏅 Tiers",             new TierCrudPanel());
        tabs.addTab("👥 Customer Segments", new CustomerSegCrudPanel());
        tabs.addTab("💼 Commissions",       new CommissionCrudPanel());

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    // ── Price List ────────────────────────────────────────────────────────────
    class PriceListCrudPanel extends CrudPanel {
        PriceListCrudPanel() { init("Price List", new String[]{"Price ID","SKU ID","Region","Channel","Type","Base Price","Floor","Currency","From","To","Status"}); }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (PriceListEntry e : facade.getAllPrices())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{e.getPriceId(), e.getSkuId(), e.getRegionCode(), e.getChannel(), e.getPriceType(), AppUtils.currency(e.getBasePrice()), AppUtils.currency(e.getPriceFloor()), e.getCurrencyCode(), e.getEffectiveFrom(), e.getEffectiveTo(), e.getStatus()}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Add Price Entry", 560, 500);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("PRC"));
            JTextField skuF  = LuxuryTheme.textField("SKU-001");
            JTextField regF  = LuxuryTheme.textField("SOUTH");
            JComboBox<String> chanCB = LuxuryTheme.comboBox(new String[]{"RETAIL","DISTRIBUTOR"});
            JComboBox<String> typCB  = LuxuryTheme.comboBox(new String[]{"RETAIL","DISTRIBUTOR"});
            JSpinner baseSp  = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 9999999.0, 0.01));
            JSpinner flrSp   = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 9999999.0, 0.01));
            JComboBox<String> curCB  = LuxuryTheme.comboBox(new String[]{"INR","USD","EUR","GBP"});
            JTextField fromF = LuxuryTheme.textField("2024-01-01 00:00:00");
            JTextField toF   = LuxuryTheme.textField("2025-12-31 23:59:59");
            JComboBox<String> stCB   = LuxuryTheme.comboBox(new String[]{"ACTIVE","INACTIVE","SUPERSEDED"});
            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Price Entry Details")
                .addField("Price ID *", idF).addField("SKU ID *", skuF).addField("Region Code *", regF)
                .addField("Channel *", chanCB).addField("Price Type *", typCB)
                .addField("Base Price (₹) *", baseSp).addField("Price Floor (₹) *", flrSp)
                .addField("Currency", curCB).addField("Effective From *", fromF).addField("Effective To *", toF)
                .addField("Status", stCB).build();
            addDialogButtons(d, form, () -> {
                PriceListEntry e = new PriceListEntry();
                e.setPriceId(idF.getText().trim()); e.setSkuId(skuF.getText().trim()); e.setRegionCode(regF.getText().trim());
                e.setChannel((String)chanCB.getSelectedItem()); e.setPriceType((String)typCB.getSelectedItem());
                e.setBasePrice((double)baseSp.getValue()); e.setPriceFloor((double)flrSp.getValue());
                e.setCurrencyCode((String)curCB.getSelectedItem()); e.setEffectiveFrom(fromF.getText().trim()); e.setEffectiveTo(toF.getText().trim());
                e.setStatus((String)stCB.getSelectedItem());
                int r = facade.createPrice(e);
                if (r > 0) { d.dispose(); loadDataAsync(); } else AppUtils.showError(d, "Failed. Check floor <= base price.");
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            PriceListEntry e = facade.getAllPrices().stream().filter(p -> p.getPriceId().equals(id)).findFirst().orElse(null);
            if (e == null) return;
            JDialog d = createDialog("Edit Price: " + id, 480, 320);
            JSpinner baseSp = new JSpinner(new SpinnerNumberModel(e.getBasePrice(), 0.0, 9999999.0, 0.01));
            JSpinner flrSp  = new JSpinner(new SpinnerNumberModel(e.getPriceFloor(), 0.0, 9999999.0, 0.01));
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"ACTIVE","INACTIVE","SUPERSEDED"});
            stCB.setSelectedItem(e.getStatus());
            JTextField toF = LuxuryTheme.textField(e.getEffectiveTo());
            JPanel form = new CrudPanel.FormBuilder().addField("Base Price (₹)", baseSp).addField("Floor (₹)", flrSp).addField("Status", stCB).addField("Effective To", toF).build();
            addDialogButtons(d, form, () -> { e.setBasePrice((double)baseSp.getValue()); e.setPriceFloor((double)flrSp.getValue()); e.setStatus((String)stCB.getSelectedItem()); e.setEffectiveTo(toF.getText().trim()); facade.updatePrice(e); d.dispose(); loadDataAsync(); });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deletePrice((String)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }

    // ── Promotions ────────────────────────────────────────────────────────────
    class PromotionCrudPanel extends CrudPanel {
        PromotionCrudPanel() { init("Promotions", new String[]{"Promo ID","Name","Coupon","Type","Value","Start","End","Min Cart","Max Uses","Used"}); }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (Promotion p : facade.getAllPromotions())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{p.getPromoId(), p.getPromoName(), p.getCouponCode(), p.getDiscountType(), p.getDiscountValue(), p.getStartDate(), p.getEndDate(), p.getMinCartValue(), p.getMaxUses(), p.getCurrentUseCount()}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Create Promotion", 520, 460);
            JTextField idF    = LuxuryTheme.textField(AppUtils.newId("PROMO"));
            JTextField nameF  = LuxuryTheme.textField("Summer Sale");
            JTextField couF   = LuxuryTheme.textField("SUMMER20");
            JComboBox<String> typCB = LuxuryTheme.comboBox(new String[]{"PERCENTAGE_OFF","FIXED_AMOUNT","BUY_X_GET_Y"});
            JSpinner valSp    = new JSpinner(new SpinnerNumberModel(10.0, 0.01, 9999.0, 0.01));
            JTextField startF = LuxuryTheme.textField("2024-01-01 00:00:00");
            JTextField endF   = LuxuryTheme.textField("2024-12-31 23:59:59");
            JSpinner minSp    = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 999999.0, 0.01));
            JSpinner maxSp    = LuxuryTheme.spinner(1, 99999, 100);
            JPanel form = new CrudPanel.FormBuilder()
                .addSeparator("Promotion Details")
                .addField("Promo ID *", idF).addField("Name *", nameF).addField("Coupon Code *", couF)
                .addField("Discount Type *", typCB).addField("Discount Value *", valSp)
                .addField("Start Date *", startF).addField("End Date *", endF)
                .addField("Min Cart Value (₹)", minSp).addField("Max Uses *", maxSp).build();
            addDialogButtons(d, form, () -> {
                Promotion p = new Promotion();
                p.setPromoId(idF.getText().trim()); p.setPromoName(nameF.getText().trim()); p.setCouponCode(couF.getText().trim());
                p.setDiscountType((String)typCB.getSelectedItem()); p.setDiscountValue((double)valSp.getValue());
                p.setStartDate(startF.getText().trim()); p.setEndDate(endF.getText().trim());
                p.setMinCartValue((double)minSp.getValue()); p.setMaxUses((int)maxSp.getValue());
                int r = facade.createPromotion(p);
                if (r > 0) { d.dispose(); loadDataAsync(); } else AppUtils.showError(d, "Failed. Check for duplicate coupon code.");
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            Promotion p = facade.getAllPromotions().stream().filter(pr -> pr.getPromoId().equals(id)).findFirst().orElse(null);
            if (p == null) return;
            JDialog d = createDialog("Edit Promotion: " + id, 480, 360);
            JTextField nameF = LuxuryTheme.textField(p.getPromoName());
            JTextField couF  = LuxuryTheme.textField(p.getCouponCode());
            JSpinner valSp   = new JSpinner(new SpinnerNumberModel(p.getDiscountValue(), 0.01, 9999.0, 0.01));
            JTextField endF  = LuxuryTheme.textField(p.getEndDate());
            JSpinner maxSp   = LuxuryTheme.spinner(1, 99999, p.getMaxUses());
            JPanel form = new CrudPanel.FormBuilder().addField("Name", nameF).addField("Coupon Code", couF).addField("Value", valSp).addField("End Date", endF).addField("Max Uses", maxSp).build();
            addDialogButtons(d, form, () -> { p.setPromoName(nameF.getText()); p.setCouponCode(couF.getText()); p.setDiscountValue((double)valSp.getValue()); p.setEndDate(endF.getText()); p.setMaxUses((int)maxSp.getValue()); facade.updatePromotion(p); d.dispose(); loadDataAsync(); });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deletePromotion((String)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }

    // ── Discount Policies ────────────────────────────────────────────────────
    class DiscountPolicyCrudPanel extends CrudPanel {
        DiscountPolicyCrudPanel() { init("Discount Policies", new String[]{"Policy ID","Name","Stacking","Priority","Max Discount %","Perishability Days","Clearance %"}); }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (DiscountPolicy d : facade.getAllDiscountPolicies())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{d.getPolicyId(), d.getPolicyName(), d.getStackingRule(), d.getPriorityLevel(), AppUtils.percent(d.getMaxDiscountCapPct()), d.getPerishabilityDays(), AppUtils.percent(d.getClearanceDiscountPct())}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Add Discount Policy", 500, 400);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("POL"));
            JTextField nameF = LuxuryTheme.textField("Policy Name");
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"EXCLUSIVE","ADDITIVE"});
            JSpinner priSp   = LuxuryTheme.spinner(1, 999, 1);
            JSpinner capSp   = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 100.0, 0.01));
            JSpinner perSp   = LuxuryTheme.spinner(1, 365, 7);
            JSpinner clrSp   = new JSpinner(new SpinnerNumberModel(20.0, 0.0, 100.0, 0.01));
            JPanel form = new CrudPanel.FormBuilder()
                .addField("Policy ID *", idF).addField("Name *", nameF).addField("Stacking Rule *", stCB)
                .addField("Priority *", priSp).addField("Max Discount Cap %", capSp)
                .addField("Perishability Days *", perSp).addField("Clearance Discount %", clrSp).build();
            addDialogButtons(d, form, () -> {
                DiscountPolicy dp = new DiscountPolicy();
                dp.setPolicyId(idF.getText().trim()); dp.setPolicyName(nameF.getText().trim()); dp.setStackingRule((String)stCB.getSelectedItem());
                dp.setPriorityLevel((int)priSp.getValue()); dp.setMaxDiscountCapPct((double)capSp.getValue());
                dp.setPerishabilityDays((int)perSp.getValue()); dp.setClearanceDiscountPct((double)clrSp.getValue());
                facade.createDiscountPolicy(dp); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            DiscountPolicy dp = facade.getAllDiscountPolicies().stream().filter(p -> p.getPolicyId().equals(id)).findFirst().orElse(null);
            if (dp == null) return;
            JDialog d = createDialog("Edit Policy: " + id, 460, 320);
            JTextField nameF = LuxuryTheme.textField(dp.getPolicyName());
            JSpinner capSp   = new JSpinner(new SpinnerNumberModel(dp.getMaxDiscountCapPct(), 0.0, 100.0, 0.01));
            JSpinner perSp   = LuxuryTheme.spinner(1, 365, dp.getPerishabilityDays());
            JSpinner clrSp   = new JSpinner(new SpinnerNumberModel(dp.getClearanceDiscountPct(), 0.0, 100.0, 0.01));
            JPanel form = new CrudPanel.FormBuilder().addField("Name", nameF).addField("Max Discount %", capSp).addField("Perishability Days", perSp).addField("Clearance %", clrSp).build();
            addDialogButtons(d, form, () -> { dp.setPolicyName(nameF.getText()); dp.setMaxDiscountCapPct((double)capSp.getValue()); dp.setPerishabilityDays((int)perSp.getValue()); dp.setClearanceDiscountPct((double)clrSp.getValue()); facade.updateDiscountPolicy(dp); d.dispose(); loadDataAsync(); });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deleteDiscountPolicy((String)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }

    // ── Contract Pricing ─────────────────────────────────────────────────────
    class ContractCrudPanel extends CrudPanel {
        ContractCrudPanel() { init("B2B Contracts", new String[]{"Contract ID","Customer ID","SKU ID","Negotiated Price","Start Date","Expiry Date","Status"}); }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (ContractPricing c : facade.getAllContracts())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{c.getContractId(), c.getContractCustomerId(), c.getContractSkuId(), AppUtils.currency(c.getNegotiatedPrice()), c.getContractStartDate(), c.getContractExpiryDate(), c.getContractStatus()}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Add Contract", 500, 380);
            JTextField idF  = LuxuryTheme.textField(AppUtils.newId("CON"));
            JTextField custF = LuxuryTheme.textField("CUST-001");
            JTextField skuF  = LuxuryTheme.textField("SKU-001");
            JSpinner priceSp = new JSpinner(new SpinnerNumberModel(0.0, 0.01, 9999999.0, 0.01));
            JTextField startF = LuxuryTheme.textField("2024-01-01 00:00:00");
            JTextField expF   = LuxuryTheme.textField("2025-12-31 23:59:59");
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"PENDING","ACTIVE","EXPIRED"});
            JPanel form = new CrudPanel.FormBuilder()
                .addField("Contract ID *", idF).addField("Customer ID *", custF).addField("SKU ID *", skuF)
                .addField("Negotiated Price (₹) *", priceSp).addField("Start Date *", startF).addField("Expiry Date *", expF).addField("Status", stCB).build();
            addDialogButtons(d, form, () -> {
                ContractPricing c = new ContractPricing();
                c.setContractId(idF.getText().trim()); c.setContractCustomerId(custF.getText().trim()); c.setContractSkuId(skuF.getText().trim());
                c.setNegotiatedPrice((double)priceSp.getValue()); c.setContractStartDate(startF.getText().trim()); c.setContractExpiryDate(expF.getText().trim()); c.setContractStatus((String)stCB.getSelectedItem());
                facade.createContract(c); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            ContractPricing c = facade.getAllContracts().stream().filter(ct -> ct.getContractId().equals(id)).findFirst().orElse(null);
            if (c == null) return;
            JDialog d = createDialog("Edit Contract: " + id, 440, 260);
            JSpinner priceSp = new JSpinner(new SpinnerNumberModel(c.getNegotiatedPrice(), 0.01, 9999999.0, 0.01));
            JTextField expF  = LuxuryTheme.textField(c.getContractExpiryDate());
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"PENDING","ACTIVE","EXPIRED"});
            stCB.setSelectedItem(c.getContractStatus());
            JPanel form = new CrudPanel.FormBuilder().addField("Negotiated Price (₹)", priceSp).addField("Expiry Date", expF).addField("Status", stCB).build();
            addDialogButtons(d, form, () -> { c.setNegotiatedPrice((double)priceSp.getValue()); c.setContractExpiryDate(expF.getText()); c.setContractStatus((String)stCB.getSelectedItem()); facade.updateContract(c); d.dispose(); loadDataAsync(); });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deleteContract((String)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }

    // ── Price Approvals ───────────────────────────────────────────────────────
    class ApprovalCrudPanel extends CrudPanel {
        ApprovalCrudPanel() { init("Price Approvals", new String[]{"Approval ID","Request Type","Requested By","Discount Amt","Status","Manager ID","Created"}); }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (PriceApproval a : facade.getAllPriceApprovals())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{a.getApprovalId(), a.getRequestType(), a.getRequestedBy(), AppUtils.currency(a.getRequestedDiscountAmt()), a.getApprovalStatus(), a.getApprovingManagerId(), a.getCreatedAt()}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Request Price Approval", 500, 380);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("APR"));
            JComboBox<String> typCB = LuxuryTheme.comboBox(new String[]{"MANUAL_DISCOUNT","CONTRACT_BYPASS","POLICY_EXCEPTION"});
            JTextField reqByF = LuxuryTheme.textField("EMP-001");
            JSpinner amtSp   = new JSpinner(new SpinnerNumberModel(0.01, 0.01, 999999.0, 0.01));
            JTextArea justTA = LuxuryTheme.textArea(4, 30);
            JPanel form = new CrudPanel.FormBuilder()
                .addField("Approval ID *", idF).addField("Request Type *", typCB).addField("Requested By *", reqByF)
                .addField("Discount Amount (₹) *", amtSp).addField("Justification *", new JScrollPane(justTA)).build();
            addDialogButtons(d, form, () -> {
                PriceApproval a = new PriceApproval();
                a.setApprovalId(idF.getText().trim()); a.setRequestType((String)typCB.getSelectedItem()); a.setRequestedBy(reqByF.getText().trim());
                a.setRequestedDiscountAmt((double)amtSp.getValue()); a.setJustificationText(justTA.getText());
                facade.createPriceApproval(a); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            JDialog d = createDialog("Update Approval Status: " + id, 440, 260);
            JComboBox<String> stCB = LuxuryTheme.comboBox(new String[]{"PENDING","APPROVED","REJECTED","ESCALATED"});
            JTextField mgrF = LuxuryTheme.textField("MGR-001");
            JPanel form = new CrudPanel.FormBuilder().addField("New Status", stCB).addField("Approving Manager ID", mgrF).build();
            addDialogButtons(d, form, () -> { facade.updateApprovalStatus(id, (String)stCB.getSelectedItem(), mgrF.getText().trim()); d.dispose(); loadDataAsync(); });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deletePriceApproval((String)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }

    // ── Tier Definitions ──────────────────────────────────────────────────────
    class TierCrudPanel extends CrudPanel {
        TierCrudPanel() { init("Customer Tiers", new String[]{"Tier ID","Name","Min Spend (₹)","Default Discount %"}); }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (TierDefinition t : facade.getAllTiers())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{t.getTierId(), t.getTierName(), AppUtils.currency(t.getMinSpendThreshold()), AppUtils.percent(t.getDefaultDiscountPct())}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Add Tier", 400, 280);
            JTextField nameF = LuxuryTheme.textField("Gold");
            JSpinner minSp   = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 9999999.0, 100.0));
            JSpinner pctSp   = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 100.0, 0.01));
            JPanel form = new CrudPanel.FormBuilder().addField("Tier Name *", nameF).addField("Min Spend (₹) *", minSp).addField("Default Discount %", pctSp).build();
            addDialogButtons(d, form, () -> {
                TierDefinition t = new TierDefinition(); t.setTierName(nameF.getText().trim()); t.setMinSpendThreshold((double)minSp.getValue()); t.setDefaultDiscountPct((double)pctSp.getValue());
                facade.createTier(t); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            int id = (int) tableModel.getValueAt(row, 0);
            TierDefinition t = facade.getAllTiers().stream().filter(td -> td.getTierId() == id).findFirst().orElse(null);
            if (t == null) return;
            JDialog d = createDialog("Edit Tier: " + id, 380, 240);
            JTextField nameF = LuxuryTheme.textField(t.getTierName());
            JSpinner pctSp = new JSpinner(new SpinnerNumberModel(t.getDefaultDiscountPct(), 0.0, 100.0, 0.01));
            JPanel form = new CrudPanel.FormBuilder().addField("Name", nameF).addField("Discount %", pctSp).build();
            addDialogButtons(d, form, () -> { t.setTierName(nameF.getText()); t.setDefaultDiscountPct((double)pctSp.getValue()); facade.updateTier(t); d.dispose(); loadDataAsync(); });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deleteTier((int)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }

    // ── Customer Segments ─────────────────────────────────────────────────────
    class CustomerSegCrudPanel extends CrudPanel {
        CustomerSegCrudPanel() { init("Customer Segments", new String[]{"Segment ID","Customer ID","Cumulative Spend","Assigned Tier ID","Manual Override","Override Tier"}); }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (CustomerSegmentation c : facade.getAllCustomerSegments())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{c.getSegmentationId(), c.getCustomerId(), AppUtils.currency(c.getCumulativeSpend()), c.getAssignedTierId(), c.isManualOverride(), c.getOverrideTierId()}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Add Customer Segment", 460, 340);
            JTextField idF   = LuxuryTheme.textField(AppUtils.newId("SEG"));
            JTextField custF = LuxuryTheme.textField("CUST-001");
            JSpinner spendSp = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 9999999.0, 100.0));
            JSpinner tierSp  = LuxuryTheme.spinner(1, 999, 1);
            JCheckBox manChk = new JCheckBox("Manual Override"); manChk.setForeground(LuxuryTheme.TEXT_PRIMARY); manChk.setOpaque(false);
            JPanel form = new CrudPanel.FormBuilder().addField("Segment ID *", idF).addField("Customer ID *", custF).addField("Cumulative Spend (₹)", spendSp).addField("Assigned Tier ID *", tierSp).addField("Options", manChk).build();
            addDialogButtons(d, form, () -> {
                CustomerSegmentation c = new CustomerSegmentation();
                c.setSegmentationId(idF.getText().trim()); c.setCustomerId(custF.getText().trim()); c.setCumulativeSpend((double)spendSp.getValue()); c.setAssignedTierId((int)tierSp.getValue()); c.setManualOverride(manChk.isSelected());
                facade.createCustomerSegment(c); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            CustomerSegmentation c = facade.getAllCustomerSegments().stream().filter(cs -> cs.getSegmentationId().equals(id)).findFirst().orElse(null);
            if (c == null) return;
            JDialog d = createDialog("Edit Segment: " + id, 440, 260);
            JSpinner spendSp = new JSpinner(new SpinnerNumberModel(c.getCumulativeSpend(), 0.0, 9999999.0, 100.0));
            JSpinner tierSp  = LuxuryTheme.spinner(1, 999, c.getAssignedTierId());
            JPanel form = new CrudPanel.FormBuilder().addField("Cumulative Spend (₹)", spendSp).addField("Assigned Tier ID", tierSp).build();
            addDialogButtons(d, form, () -> { c.setCumulativeSpend((double)spendSp.getValue()); c.setAssignedTierId((int)tierSp.getValue()); facade.updateCustomerSegment(c); d.dispose(); loadDataAsync(); });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deleteCustomerSegment((String)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }

    // ── Commissions ───────────────────────────────────────────────────────────
    class CommissionCrudPanel extends CrudPanel {
        CommissionCrudPanel() { init("Commission Ledger", new String[]{"Commission ID","Agent ID","Period Start","Period End","Total Sales (₹)","Total Commission (₹)","Calculated At"}); }

        @Override protected void loadData() {
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            for (CommissionEntry c : facade.getAllCommissions())
                SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{c.getCommissionId(), c.getAgentId(), c.getPeriodStart(), c.getPeriodEnd(), AppUtils.currency(c.getTotalSales()), AppUtils.currency(c.getTotalCommission()), c.getCalculatedAt()}));
            SwingUtilities.invokeLater(() -> updateRecordCount());
        }

        @Override protected void openAddDialog() {
            JDialog d = createDialog("Record Commission", 480, 360);
            JTextField idF    = LuxuryTheme.textField(AppUtils.newId("COM"));
            JTextField agentF = LuxuryTheme.textField("AGENT-001");
            JTextField startF = LuxuryTheme.textField("2024-01-01");
            JTextField endF   = LuxuryTheme.textField("2024-03-31");
            JSpinner salesSp  = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 9999999.0, 100.0));
            JSpinner comSp    = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 9999999.0, 10.0));
            JPanel form = new CrudPanel.FormBuilder()
                .addField("Commission ID *", idF).addField("Agent ID *", agentF).addField("Period Start *", startF)
                .addField("Period End *", endF).addField("Total Sales (₹) *", salesSp).addField("Total Commission (₹) *", comSp).build();
            addDialogButtons(d, form, () -> {
                CommissionEntry c = new CommissionEntry();
                c.setCommissionId(idF.getText().trim()); c.setAgentId(agentF.getText().trim()); c.setPeriodStart(startF.getText().trim()); c.setPeriodEnd(endF.getText().trim()); c.setTotalSales((double)salesSp.getValue()); c.setTotalCommission((double)comSp.getValue());
                facade.createCommission(c); d.dispose(); loadDataAsync();
            });
            d.setVisible(true);
        }

        @Override protected void openEditDialog(int row) {
            String id = (String) tableModel.getValueAt(row, 0);
            CommissionEntry c = facade.getAllCommissions().stream().filter(cm -> cm.getCommissionId().equals(id)).findFirst().orElse(null);
            if (c == null) return;
            JDialog d = createDialog("Edit Commission: " + id, 440, 240);
            JSpinner salesSp = new JSpinner(new SpinnerNumberModel(c.getTotalSales(), 0.0, 9999999.0, 100.0));
            JSpinner comSp   = new JSpinner(new SpinnerNumberModel(c.getTotalCommission(), 0.0, 9999999.0, 10.0));
            JPanel form = new CrudPanel.FormBuilder().addField("Total Sales (₹)", salesSp).addField("Total Commission (₹)", comSp).build();
            addDialogButtons(d, form, () -> { c.setTotalSales((double)salesSp.getValue()); c.setTotalCommission((double)comSp.getValue()); facade.updateCommission(c); d.dispose(); loadDataAsync(); });
            d.setVisible(true);
        }

        @Override protected void deleteRow(int row) { facade.deleteCommission((String)tableModel.getValueAt(row,0)); loadDataAsync(); }
    }
}
