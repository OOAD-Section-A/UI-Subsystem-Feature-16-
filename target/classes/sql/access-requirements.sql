-- Subsystem access requirements for the pricing-related additions.
-- Apply this manually using a privileged MySQL account after schema creation.
-- The application bootstrap does not execute GRANT statements.
--
-- Note: the user request referenced `customer_tier_cash`; this project
-- interprets that as `customer_tier_cache`.

CREATE ROLE IF NOT EXISTS `pricing_discount_rw`;
CREATE ROLE IF NOT EXISTS `packing_repairs_receipt_ro`;
CREATE ROLE IF NOT EXISTS `reporting_dashboard_ro`;
CREATE ROLE IF NOT EXISTS `warehouse_management_rw`;

GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`promotion_eligible_skus` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`bundle_promotions` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`bundle_promotion_skus` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`rebate_programs` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`volume_discount_schedules` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`volume_tier_rules` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`customer_tier_cache` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`customer_tier_overrides` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`regional_pricing_multipliers` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`contracts` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`contract_sku_prices` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`approval_requests` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`audit_log` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`profitability_analytics` TO `pricing_discount_rw`;

GRANT SELECT ON `OOAD`.`contract_sku_prices` TO `packing_repairs_receipt_ro`;
GRANT SELECT ON `OOAD`.`discount_policies` TO `packing_repairs_receipt_ro`;
GRANT SELECT ON `OOAD`.`bundle_promotions` TO `packing_repairs_receipt_ro`;
GRANT SELECT ON `OOAD`.`promotions` TO `packing_repairs_receipt_ro`;
GRANT SELECT ON `OOAD`.`promotion_eligible_skus` TO `packing_repairs_receipt_ro`;
GRANT SELECT ON `OOAD`.`bundle_promotion_skus` TO `packing_repairs_receipt_ro`;
GRANT SELECT ON `OOAD`.`price_list` TO `packing_repairs_receipt_ro`;

GRANT SELECT ON `OOAD`.`customer_tier_cache` TO `reporting_dashboard_ro`;
GRANT SELECT ON `OOAD`.`price_list` TO `reporting_dashboard_ro`;
GRANT SELECT ON `OOAD`.`promotions` TO `reporting_dashboard_ro`;
GRANT SELECT ON `OOAD`.`bundle_promotions` TO `reporting_dashboard_ro`;
GRANT SELECT ON `OOAD`.`discount_policies` TO `reporting_dashboard_ro`;
GRANT SELECT ON `OOAD`.`rebate_programs` TO `reporting_dashboard_ro`;
GRANT SELECT ON `OOAD`.`regional_pricing_multipliers` TO `reporting_dashboard_ro`;

-- WMS execution tables are owned by Warehouse Management.
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`wms_storage_units_lpn` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE ON `OOAD`.`wms_pick_waves` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`wms_task_queue` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`warehouse_zones` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`bins` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE ON `OOAD`.`goods_receipts` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`stock_records` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`stock_movements` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`pick_tasks` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`staging_dispatch` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`warehouse_returns` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`cycle_counts` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`proc_suppliers` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`proc_product_supplier` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE ON `OOAD`.`proc_purchase_orders` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`proc_po_items` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`proc_asn` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`proc_quality_inspections` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`proc_supplier_invoices` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`proc_invoice_items` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`proc_supplier_payments` TO `warehouse_management_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`proc_discrepancies` TO `warehouse_management_rw`;
