INSERT INTO warehouses (warehouse_id, warehouse_name)
VALUES ('WH-001', 'Central Warehouse')
ON DUPLICATE KEY UPDATE warehouse_name = VALUES(warehouse_name);

INSERT INTO orders (
    order_id, customer_id, order_status, order_date, total_amount, payment_status, sales_channel
) VALUES (
    'ORD-1001', 'CUST-77', 'CONFIRMED', NOW(), 420.00, 'PAID', 'ONLINE'
)
ON DUPLICATE KEY UPDATE order_status = VALUES(order_status), total_amount = VALUES(total_amount);

INSERT INTO order_items (
    order_item_id, order_id, product_id, ordered_quantity, unit_price, line_total
) VALUES (
    'ITEM-1001', 'ORD-1001', 'PROD-APPLE-001', 3, 140.00, 420.00
)
ON DUPLICATE KEY UPDATE ordered_quantity = VALUES(ordered_quantity), line_total = VALUES(line_total);

INSERT INTO price_list (
    price_id, sku_id, region_code, channel, price_type, base_price, price_floor,
    currency_code, effective_from, effective_to, status
) VALUES (
    'PRICE-001', 'SKU-APPLE-001', 'SOUTH', 'RETAIL', 'RETAIL', 120.00, 100.00,
    'INR', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE'
)
ON DUPLICATE KEY UPDATE base_price = VALUES(base_price), price_floor = VALUES(price_floor);

INSERT INTO delivery_orders (
    delivery_id, order_id, customer_id, delivery_address, delivery_status, delivery_type,
    delivery_cost, assigned_agent, warehouse_id, created_at, updated_at
) VALUES (
    'SHIP-001', 'ORD-1001', 'CUST-77', 'Bengaluru, Karnataka', 'PENDING', 'STANDARD',
    180.00, 'AGENT-22', 'WH-001', NOW(), NOW()
)
ON DUPLICATE KEY UPDATE delivery_status = VALUES(delivery_status), updated_at = VALUES(updated_at);

INSERT INTO demand_forecasts (
    forecast_id, product_id, forecast_period, predicted_demand, confidence_score, generated_at, source_event_reference
) VALUES (
    'DF-001', 'PROD-APPLE-001', '2026-Q2', 850, 92.50, NOW(), 'BAR-001'
)
ON DUPLICATE KEY UPDATE predicted_demand = VALUES(predicted_demand), confidence_score = VALUES(confidence_score);

INSERT INTO sales_records (
    sale_id, product_id, store_id, sale_date, quantity_sold, unit_price, revenue, region
) VALUES (
    'SALE-001', 'PROD-APPLE-001', 'STORE-01', CURDATE(), 10, 140.00, 1400.00, 'SOUTH'
)
ON DUPLICATE KEY UPDATE quantity_sold = VALUES(quantity_sold), revenue = VALUES(revenue);

INSERT INTO holiday_calendar (
    holiday_id, holiday_date, holiday_name, holiday_type, region_applicable
) VALUES (
    'HOL-001', CURDATE(), 'Regional Festival', 'PUBLIC', 'SOUTH'
)
ON DUPLICATE KEY UPDATE holiday_name = VALUES(holiday_name);

INSERT INTO promotional_calendar (
    promo_calendar_id, promo_id, promo_name, promo_start_date, promo_end_date, discount_percentage, promo_type, applicable_products
) VALUES (
    'PC-001', 'PROMO-APPLE', 'Apple Fest', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 10.00, 'SEASONAL', 'PROD-APPLE-001'
)
ON DUPLICATE KEY UPDATE promo_name = VALUES(promo_name), discount_percentage = VALUES(discount_percentage);

INSERT INTO product_lifecycle_stages (
    lifecycle_id, product_id, current_stage, stage_start_date, previous_stage, transition_date
) VALUES (
    'LC-001', 'PROD-APPLE-001', 'GROWTH', CURDATE(), 'INTRODUCTION', CURDATE()
)
ON DUPLICATE KEY UPDATE current_stage = VALUES(current_stage);

INSERT INTO forecast_performance_metrics (
    eval_id, forecast_id, forecast_date, predicted_qty, actual_qty, mape, rmse, model_used
) VALUES (
    'FPM-001', 'DF-001', CURDATE(), 850, 820, 3.66, 12.4200, 'ARIMA'
)
ON DUPLICATE KEY UPDATE actual_qty = VALUES(actual_qty), mape = VALUES(mape), rmse = VALUES(rmse);

INSERT INTO barcode_rfid_events (
    event_id, product_id, event_type, source_device, warehouse_id, event_timestamp, raw_payload
) VALUES (
    'BAR-001', 'PROD-APPLE-001', 'SCAN_IN', 'RFID_GATE_A1', 'WH-001', NOW(), '{"tag":"RFID-7788"}'
)
ON DUPLICATE KEY UPDATE raw_payload = VALUES(raw_payload), event_timestamp = VALUES(event_timestamp);

INSERT INTO packaging_jobs (
    package_id, order_id, quantity, total_amount, discounts, packaging_status, packed_by, created_at
) VALUES (
    'PKG-001', 'ORD-1001', 3, 420.00, 0.00, 'PACKED', 'EMP-01', NOW()
)
ON DUPLICATE KEY UPDATE packaging_status = VALUES(packaging_status);

INSERT INTO receipt_records (
    receipt_record_id, order_id, package_id, received_amount, receipt_status, recorded_at
) VALUES (
    'REC-001', 'ORD-1001', 'PKG-001', 420.00, 'RECEIVED', NOW()
)
ON DUPLICATE KEY UPDATE receipt_status = VALUES(receipt_status);

INSERT INTO repair_requests (
    request_id, order_id, product_id, defect_details, request_status, requested_at
) VALUES (
    'REP-001', 'ORD-1001', 'PROD-APPLE-001', 'Minor packaging tear observed', 'OPEN', NOW()
)
ON DUPLICATE KEY UPDATE request_status = VALUES(request_status);

INSERT INTO product_returns (
    return_request_id, order_id, customer_id, product_details, defect_details, customer_feedback, transport_details,
    warranty_valid_until, return_status, created_at
) VALUES (
    'RET-001', 'ORD-1001', 'CUST-77', 'Fresh apples - 3 units', 'Bruising on delivery', 'Customer requested replacement',
    'Reverse pickup requested', DATE_ADD(NOW(), INTERVAL 30 DAY), 'INITIATED', NOW()
)
ON DUPLICATE KEY UPDATE return_status = VALUES(return_status), customer_feedback = VALUES(customer_feedback);

INSERT INTO return_growth_statistics (
    growth_stat_id, return_request_id, metric_period, return_rate, resolution_rate, recorded_at
) VALUES (
    'RGS-001', 'RET-001', '2026-Q2', 4.50, 92.00, NOW()
)
ON DUPLICATE KEY UPDATE return_rate = VALUES(return_rate), resolution_rate = VALUES(resolution_rate);

INSERT INTO shipments (
    shipment_id, order_id, origin_address, destination_address, package_weight, is_drop_ship, shipping_priority,
    shipment_status, supplier_id, inventory_level, route_id, carrier_id, tracking_id, min_cost_constraint,
    min_time_constraint, avoid_tolls_constraint, calculated_cost, created_at
) VALUES (
    'SHP-001', 'ORD-1001', 'Central Warehouse, Bengaluru', 'Bengaluru, Karnataka', 12.50, FALSE, 'HIGH',
    'IN_TRANSIT', 'SUP-01', 150, 'ROUTE-001', 'CARRIER-01', 'TRK-001', TRUE,
    FALSE, TRUE, 280.00, NOW()
)
ON DUPLICATE KEY UPDATE shipment_status = VALUES(shipment_status), calculated_cost = VALUES(calculated_cost);

INSERT INTO logistics_routes (
    route_id, shipment_id, current_eta, timeline_stage, route_status, requires_rerouting
) VALUES (
    'ROUTE-001', 'SHP-001', DATE_ADD(NOW(), INTERVAL 6 HOUR), 'EN_ROUTE', 'ACTIVE', FALSE
)
ON DUPLICATE KEY UPDATE route_status = VALUES(route_status), current_eta = VALUES(current_eta);

INSERT INTO shipment_alerts (
    alert_id, shipment_id, alert_message, alert_severity, created_at
) VALUES (
    'ALERT-001', 'SHP-001', 'Traffic congestion detected on planned route', 'MEDIUM', NOW()
)
ON DUPLICATE KEY UPDATE alert_message = VALUES(alert_message);

INSERT INTO delivery_tracking_routes (
    route_plan_id, delivery_id, carrier_id, tracking_api_url, planned_departure, planned_arrival, current_eta, route_status
) VALUES (
    'DR-001', 'SHIP-001', 'CARRIER-01', 'https://tracking.example/api/SHIP-001', NOW(), DATE_ADD(NOW(), INTERVAL 8 HOUR),
    DATE_ADD(NOW(), INTERVAL 9 HOUR), 'IN_PROGRESS'
)
ON DUPLICATE KEY UPDATE current_eta = VALUES(current_eta), route_status = VALUES(route_status);

INSERT INTO delivery_tracking_waypoints (
    waypoint_id, route_plan_id, waypoint_sequence, waypoint_location
) VALUES (
    'WP-001', 'DR-001', 1, 'Warehouse Exit Gate'
)
ON DUPLICATE KEY UPDATE waypoint_location = VALUES(waypoint_location);

INSERT INTO delivery_tracking_events (
    tracking_event_id, delivery_id, rider_id, vehicle_id, timeline_stage, gps_coordinates, event_timestamp, alert_message, requires_rerouting
) VALUES (
    'DTE-001', 'SHIP-001', 'RIDER-01', 'VEH-01', 'DISPATCHED', '12.9716,77.5946', NOW(), NULL, FALSE
)
ON DUPLICATE KEY UPDATE timeline_stage = VALUES(timeline_stage), gps_coordinates = VALUES(gps_coordinates);

INSERT INTO subsystem_exceptions (
    exception_id, subsystem_name, reference_id, severity, exception_message, status, created_at, resolved_at
) VALUES (
    'EX-001', 'DeliveryTracking', 'SHIP-001', 'HIGH', 'Shipment delayed due to route reassignment', 'OPEN', NOW(), NULL
)
ON DUPLICATE KEY UPDATE status = VALUES(status), exception_message = VALUES(exception_message);

-- ============================================================
--  SCM Supply Chain Management — Comprehensive Dummy Data
--  Covers ALL tables in schema.sql + schema-extension.sql
--  Respects all FK relationships and CHECK constraints
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
USE OOAD;

-- ============================================================
-- SUBSYSTEM 1 — MULTI-LEVEL PRICING & DISCOUNT MANAGEMENT
-- ============================================================

-- tier_definitions (must come before customer_segmentation)
INSERT INTO tier_definitions (tier_name, min_spend_threshold, default_discount_pct) VALUES
('Bronze',   0.00,    2.00),
('Silver',   5000.00, 5.00),
('Gold',     15000.00, 10.00),
('Platinum', 50000.00, 15.00)
ON DUPLICATE KEY UPDATE default_discount_pct = VALUES(default_discount_pct);

-- price_list
INSERT INTO price_list (price_id, sku_id, region_code, channel, price_type, base_price, price_floor, currency_code, effective_from, effective_to, status) VALUES
('PRICE-001', 'SKU-APPLE-001',  'SOUTH', 'RETAIL',       'RETAIL',       120.00, 100.00, 'INR', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY),  'ACTIVE'),
('PRICE-002', 'SKU-MANGO-001',  'SOUTH', 'RETAIL',       'RETAIL',       80.00,  65.00,  'INR', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY),  'ACTIVE'),
('PRICE-003', 'SKU-RICE-001',   'NORTH', 'DISTRIBUTOR',  'DISTRIBUTOR',  55.00,  45.00,  'INR', NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY),  'ACTIVE'),
('PRICE-004', 'SKU-WHEAT-001',  'NORTH', 'RETAIL',       'RETAIL',       42.00,  35.00,  'INR', NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY),  'ACTIVE'),
('PRICE-005', 'SKU-MILK-001',   'WEST',  'RETAIL',       'RETAIL',       28.00,  22.00,  'INR', NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY),  'ACTIVE'),
('PRICE-006', 'SKU-SUGAR-001',  'EAST',  'DISTRIBUTOR',  'DISTRIBUTOR',  44.00,  38.00,  'INR', NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY),  'ACTIVE'),
('PRICE-007', 'SKU-OIL-001',    'SOUTH', 'RETAIL',       'RETAIL',       145.00, 125.00, 'INR', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY),  'ACTIVE'),
('PRICE-008', 'SKU-APPLE-001',  'NORTH', 'RETAIL',       'RETAIL',       130.00, 110.00, 'INR', DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'SUPERSEDED'),
('PRICE-009', 'SKU-BANANA-001', 'SOUTH', 'RETAIL',       'RETAIL',       35.00,  28.00,  'INR', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY),  'ACTIVE'),
('PRICE-010', 'SKU-POTATO-001', 'WEST',  'DISTRIBUTOR',  'DISTRIBUTOR',  22.00,  18.00,  'INR', NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY),  'ACTIVE')
ON DUPLICATE KEY UPDATE base_price = VALUES(base_price);

-- customer_segmentation
INSERT INTO customer_segmentation (segmentation_id, customer_id, cumulative_spend, historical_order_totals, assigned_tier_id, manual_override, override_tier_id) VALUES
('SEG-001', 'CUST-77',  420.00,    420.00,    1, FALSE, NULL),
('SEG-002', 'CUST-12',  8500.00,   8500.00,   2, FALSE, NULL),
('SEG-003', 'CUST-33',  22000.00,  22000.00,  3, FALSE, NULL),
('SEG-004', 'CUST-44',  1200.00,   1200.00,   1, TRUE,  2),
('SEG-005', 'CUST-55',  55000.00,  55000.00,  4, FALSE, NULL),
('SEG-006', 'CUST-66',  3200.00,   3200.00,   1, FALSE, NULL),
('SEG-007', 'CUST-88',  11000.00,  11000.00,  2, FALSE, NULL),
('SEG-008', 'CUST-99',  780.00,    780.00,    1, FALSE, NULL)
ON DUPLICATE KEY UPDATE cumulative_spend = VALUES(cumulative_spend);

-- price_configuration
INSERT INTO price_configuration (price_config_id, sku_id, cogs_value, desired_margin_pct, computed_base_price, product_attributes, created_at) VALUES
('PC-001', 'SKU-APPLE-001',  80.00, 33.33, 120.00, '{"weight":"1kg","type":"Fresh Fruit","origin":"Himachal"}', NOW()),
('PC-002', 'SKU-MANGO-001',  55.00, 31.25, 80.00,  '{"weight":"1kg","type":"Fresh Fruit","origin":"Ratnagiri"}', NOW()),
('PC-003', 'SKU-RICE-001',   38.00, 30.91, 55.00,  '{"weight":"1kg","type":"Grain","origin":"Punjab"}',         NOW()),
('PC-004', 'SKU-WHEAT-001',  29.00, 30.95, 42.00,  '{"weight":"1kg","type":"Grain","origin":"Haryana"}',        NOW()),
('PC-005', 'SKU-MILK-001',   20.00, 28.57, 28.00,  '{"volume":"500ml","type":"Dairy","fat":"3.5%"}',            NOW()),
('PC-006', 'SKU-BANANA-001', 22.00, 37.14, 35.00,  '{"weight":"1kg","type":"Fresh Fruit","origin":"Kerala"}',   NOW()),
('PC-007', 'SKU-OIL-001',   100.00, 31.03,145.00,  '{"volume":"1L","type":"Edible Oil","variety":"Sunflower"}', NOW())
ON DUPLICATE KEY UPDATE computed_base_price = VALUES(computed_base_price);

-- discount_policies
INSERT INTO discount_policies (policy_id, policy_name, stacking_rule, priority_level, max_discount_cap_pct, perishability_days, clearance_discount_pct) VALUES
('POL-001', 'Standard Retail Policy',      'EXCLUSIVE', 1, 20.00, 5,  15.00),
('POL-002', 'Distributor Bulk Policy',     'ADDITIVE',  2, 30.00, 7,  20.00),
('POL-003', 'Clearance Markdown Policy',   'EXCLUSIVE', 3, 50.00, 2,  40.00),
('POL-004', 'Loyalty Member Policy',       'ADDITIVE',  4, 25.00, 10, 10.00),
('POL-005', 'Seasonal Campaign Policy',    'ADDITIVE',  5, 35.00, 3,  25.00)
ON DUPLICATE KEY UPDATE max_discount_cap_pct = VALUES(max_discount_cap_pct);

-- promotions
INSERT INTO promotions (promo_id, promo_name, coupon_code, discount_type, discount_value, start_date, end_date, eligible_sku_ids, min_cart_value, max_uses, current_use_count) VALUES
('PROMO-001', 'Apple Fest',         'APFEST10',  'PERCENTAGE_OFF', 10.00, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY),  '["SKU-APPLE-001"]',                       200.00, 500,  42),
('PROMO-002', 'Mango Mania',        'MANGO15',   'PERCENTAGE_OFF', 15.00, NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY), '["SKU-MANGO-001"]',                       150.00, 300,  18),
('PROMO-003', 'Grain Bonanza',      'GRAIN20',   'FIXED_AMOUNT',   20.00, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), '["SKU-RICE-001","SKU-WHEAT-001"]',         500.00, 200,  7),
('PROMO-004', 'Bundle Saver',       'BUNDLE5',   'BUY_X_GET_Y',    5.00,  NOW(), DATE_ADD(NOW(), INTERVAL 21 DAY), '["SKU-APPLE-001","SKU-BANANA-001"]',       100.00, 150,  23),
('PROMO-005', 'Dairy Delight',      'DAIRY5',    'PERCENTAGE_OFF', 5.00,  NOW(), DATE_ADD(NOW(), INTERVAL 10 DAY), '["SKU-MILK-001"]',                        50.00,  1000, 210),
('PROMO-006', 'Oil Price Drop',     'OILDROP12', 'FIXED_AMOUNT',   12.00, NOW(), DATE_ADD(NOW(), INTERVAL 5 DAY),  '["SKU-OIL-001"]',                         145.00, 100,  5)
ON DUPLICATE KEY UPDATE current_use_count = VALUES(current_use_count);

-- discount_rule_results
INSERT INTO discount_rule_results (order_line_id, order_id, quantity, batch_expiry_date, final_price, applied_discount_pct, discount_breakdown, computed_at) VALUES
('LINE-1001', 'ORD-1001', 3,  DATE_ADD(NOW(), INTERVAL 14 DAY), 126.00, 10.00, 'Promo:APFEST10 -10%',                    NOW()),
('LINE-1002', 'ORD-1002', 5,  DATE_ADD(NOW(), INTERVAL 7 DAY),  68.00,  15.00, 'Promo:MANGO15 -15%',                     NOW()),
('LINE-1003', 'ORD-1003', 20, DATE_ADD(NOW(), INTERVAL 30 DAY), 44.00,  20.00, 'Policy:GRAIN20 -INR20 + Tier:Silver -5%',NOW()),
('LINE-1004', 'ORD-1004', 2,  DATE_ADD(NOW(), INTERVAL 5 DAY),  25.00,  10.71, 'Promo:DAIRY5 -5% + Tier:Bronze -2%',     NOW()),
('LINE-1005', 'ORD-1005', 10, DATE_ADD(NOW(), INTERVAL 20 DAY), 123.25, 15.00, 'Promo:OILDROP12 + Tier:Gold -10%',       NOW())
ON DUPLICATE KEY UPDATE final_price = VALUES(final_price);

-- contract_pricing
INSERT INTO contract_pricing (contract_id, contract_customer_id, contract_sku_id, negotiated_price, contract_start_date, contract_expiry_date, contract_status) VALUES
('CONT-001', 'CUST-33', 'SKU-RICE-001',   48.00, NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY), 'ACTIVE'),
('CONT-002', 'CUST-55', 'SKU-OIL-001',   130.00, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 'ACTIVE'),
('CONT-003', 'CUST-12', 'SKU-WHEAT-001',  38.00, DATE_SUB(NOW(), INTERVAL 400 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY), 'EXPIRED'),
('CONT-004', 'CUST-88', 'SKU-APPLE-001', 110.00, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 187 DAY), 'PENDING')
ON DUPLICATE KEY UPDATE contract_status = VALUES(contract_status);

-- price_approvals
INSERT INTO price_approvals (approval_id, request_type, requested_by, requested_discount_amt, justification_text, approving_manager_id, approval_status, approval_timestamp, audit_log_flag, created_at) VALUES
('APR-001', 'MANUAL_DISCOUNT',   'EMP-05', 25.00, 'Customer loyalty reward for bulk purchase',          'MGR-01', 'APPROVED',  NOW(), TRUE,  NOW()),
('APR-002', 'CONTRACT_BYPASS',   'EMP-11', 18.00, 'Price match request from competitor quote',           'MGR-02', 'PENDING',   NULL, FALSE, NOW()),
('APR-003', 'POLICY_EXCEPTION',  'EMP-07', 40.00, 'Near-expiry clearance for perishable batch',          'MGR-01', 'APPROVED',  NOW(), TRUE,  NOW()),
('APR-004', 'MANUAL_DISCOUNT',   'EMP-03', 15.00, 'Festival discount approved by sales head',            'MGR-03', 'REJECTED',  NOW(), TRUE,  NOW()),
('APR-005', 'POLICY_EXCEPTION',  'EMP-09', 55.00, 'Emergency liquidation request — overstock clearance', 'MGR-02', 'ESCALATED', NULL, FALSE, NOW())
ON DUPLICATE KEY UPDATE approval_status = VALUES(approval_status);

-- ============================================================
-- SUBSYSTEM 2 — WAREHOUSE MANAGEMENT
-- ============================================================

-- warehouses
INSERT INTO warehouses (warehouse_id, warehouse_name) VALUES
('WH-001', 'Central Warehouse'),
('WH-002', 'North Depot'),
('WH-003', 'South Hub'),
('WH-004', 'East Fulfillment Center'),
('WH-005', 'West Cold Storage')
ON DUPLICATE KEY UPDATE warehouse_name = VALUES(warehouse_name);

-- warehouse_zones
INSERT INTO warehouse_zones (zone_id, warehouse_id, zone_type) VALUES
('ZONE-001', 'WH-001', 'STORAGE'),
('ZONE-002', 'WH-001', 'PICKING'),
('ZONE-003', 'WH-001', 'DISPATCH'),
('ZONE-004', 'WH-001', 'RECEIVING'),
('ZONE-005', 'WH-002', 'STORAGE'),
('ZONE-006', 'WH-002', 'STAGING'),
('ZONE-007', 'WH-003', 'STORAGE'),
('ZONE-008', 'WH-003', 'PICKING'),
('ZONE-009', 'WH-004', 'RECEIVING'),
('ZONE-010', 'WH-005', 'STORAGE')
ON DUPLICATE KEY UPDATE zone_type = VALUES(zone_type);

-- bins
INSERT INTO bins (bin_id, zone_id, bin_capacity, bin_status) VALUES
('BIN-A01', 'ZONE-001', 500, 'OCCUPIED'),
('BIN-A02', 'ZONE-001', 500, 'OCCUPIED'),
('BIN-A03', 'ZONE-001', 300, 'AVAILABLE'),
('BIN-B01', 'ZONE-002', 200, 'OCCUPIED'),
('BIN-B02', 'ZONE-002', 200, 'RESERVED'),
('BIN-C01', 'ZONE-003', 100, 'AVAILABLE'),
('BIN-D01', 'ZONE-004', 400, 'OCCUPIED'),
('BIN-E01', 'ZONE-005', 600, 'AVAILABLE'),
('BIN-F01', 'ZONE-007', 500, 'OCCUPIED'),
('BIN-G01', 'ZONE-010', 800, 'AVAILABLE'),
('BIN-H01', 'ZONE-006', 350, 'RESERVED'),
('BIN-I01', 'ZONE-008', 250, 'OCCUPIED')
ON DUPLICATE KEY UPDATE bin_status = VALUES(bin_status);

-- goods_receipts
INSERT INTO goods_receipts (goods_receipt_id, purchase_order_id, supplier_id, product_id, ordered_qty, received_qty, received_at, condition_status) VALUES
('GR-001', 'PO-2001', 'SUP-01', 'PROD-APPLE-001',  1000, 995,  NOW(), 'GOOD'),
('GR-002', 'PO-2002', 'SUP-02', 'PROD-MANGO-001',  500,  490,  NOW(), 'PARTIAL'),
('GR-003', 'PO-2003', 'SUP-03', 'PROD-RICE-001',   2000, 2000, NOW(), 'GOOD'),
('GR-004', 'PO-2004', 'SUP-01', 'PROD-WHEAT-001',  3000, 2980, NOW(), 'GOOD'),
('GR-005', 'PO-2005', 'SUP-04', 'PROD-MILK-001',   800,  750,  NOW(), 'GOOD'),
('GR-006', 'PO-2006', 'SUP-05', 'PROD-OIL-001',    400,  380,  NOW(), 'PARTIAL'),
('GR-007', 'PO-2007', 'SUP-02', 'PROD-BANANA-001', 600,  10,   NOW(), 'DAMAGED')
ON DUPLICATE KEY UPDATE received_qty = VALUES(received_qty);

-- stock_records
INSERT INTO stock_records (stock_id, product_id, bin_id, quantity, last_updated) VALUES
('STK-001', 'PROD-APPLE-001',  'BIN-A01', 350, NOW()),
('STK-002', 'PROD-MANGO-001',  'BIN-A02', 200, NOW()),
('STK-003', 'PROD-RICE-001',   'BIN-D01', 800, NOW()),
('STK-004', 'PROD-WHEAT-001',  'BIN-E01', 600, NOW()),
('STK-005', 'PROD-MILK-001',   'BIN-G01', 150, NOW()),
('STK-006', 'PROD-OIL-001',    'BIN-F01', 120, NOW()),
('STK-007', 'PROD-BANANA-001', 'BIN-B01', 80,  NOW()),
('STK-008', 'PROD-POTATO-001', 'BIN-I01', 300, NOW())
ON DUPLICATE KEY UPDATE quantity = VALUES(quantity);

-- stock_movements
INSERT INTO stock_movements (movement_id, movement_type, from_bin, to_bin, product_id, moved_qty, movement_ts) VALUES
('MOV-001', 'INBOUND',    NULL,     'BIN-A01', 'PROD-APPLE-001',  350, NOW()),
('MOV-002', 'INBOUND',    NULL,     'BIN-A02', 'PROD-MANGO-001',  200, NOW()),
('MOV-003', 'OUTBOUND',   'BIN-A01', NULL,     'PROD-APPLE-001',  3,   NOW()),
('MOV-004', 'TRANSFER',   'BIN-D01', 'BIN-C01','PROD-RICE-001',   50,  NOW()),
('MOV-005', 'RETURN',     NULL,     'BIN-A01', 'PROD-APPLE-001',  3,   NOW()),
('MOV-006', 'ADJUSTMENT', 'BIN-F01', NULL,     'PROD-OIL-001',    5,   NOW()),
('MOV-007', 'INBOUND',    NULL,     'BIN-I01', 'PROD-POTATO-001', 300, NOW())
ON DUPLICATE KEY UPDATE moved_qty = VALUES(moved_qty);

-- pick_tasks
INSERT INTO pick_tasks (pick_task_id, order_id, assigned_employee_id, product_id, pick_qty, task_status) VALUES
('PICK-001', 'ORD-1001', 'EMP-01', 'PROD-APPLE-001',  3,  'COMPLETED'),
('PICK-002', 'ORD-1002', 'EMP-02', 'PROD-MANGO-001',  5,  'COMPLETED'),
('PICK-003', 'ORD-1003', 'EMP-03', 'PROD-RICE-001',   20, 'IN_PROGRESS'),
('PICK-004', 'ORD-1004', 'EMP-04', 'PROD-MILK-001',   2,  'PENDING'),
('PICK-005', 'ORD-1005', 'EMP-01', 'PROD-OIL-001',    10, 'PENDING'),
('PICK-006', 'ORD-1006', 'EMP-05', 'PROD-BANANA-001', 8,  'CANCELLED')
ON DUPLICATE KEY UPDATE task_status = VALUES(task_status);

-- staging_dispatch
INSERT INTO staging_dispatch (staging_id, dock_door_id, order_id, dispatched_at, shipment_status) VALUES
('STG-001', 'DOCK-A', 'ORD-1001', NOW(),                            'DISPATCHED'),
('STG-002', 'DOCK-B', 'ORD-1002', NOW(),                            'DISPATCHED'),
('STG-003', 'DOCK-A', 'ORD-1003', NULL,                             'STAGED'),
('STG-004', 'DOCK-C', 'ORD-1004', NULL,                             'LOADED'),
('STG-005', 'DOCK-B', 'ORD-1005', NULL,                             'STAGED'),
('STG-006', 'DOCK-D', 'ORD-1006', DATE_SUB(NOW(), INTERVAL 2 DAY), 'DISPATCHED')
ON DUPLICATE KEY UPDATE shipment_status = VALUES(shipment_status);

-- warehouse_returns
INSERT INTO warehouse_returns (return_id, product_id, return_qty, condition_status, return_ts) VALUES
('WRET-001', 'PROD-APPLE-001',  3, 'DAMAGED',  NOW()),
('WRET-002', 'PROD-MANGO-001',  2, 'GOOD',     NOW()),
('WRET-003', 'PROD-RICE-001',   5, 'PARTIAL',  NOW()),
('WRET-004', 'PROD-MILK-001',   1, 'REJECTED', NOW()),
('WRET-005', 'PROD-OIL-001',    2, 'GOOD',     DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE condition_status = VALUES(condition_status);

-- cycle_counts
INSERT INTO cycle_counts (cycle_count_id, product_id, product_name, sku, employee_id, employee_name, expected_qty, counted_qty, count_ts) VALUES
('CC-001', 'PROD-APPLE-001',  'Fresh Apples',    'SKU-APPLE-001',  'EMP-03', 'Ravi Kumar',    350, 347, NOW()),
('CC-002', 'PROD-MANGO-001',  'Alphonso Mangoes','SKU-MANGO-001',  'EMP-03', 'Ravi Kumar',    200, 200, NOW()),
('CC-003', 'PROD-RICE-001',   'Basmati Rice',    'SKU-RICE-001',   'EMP-07', 'Priya Nair',    800, 795, DATE_SUB(NOW(), INTERVAL 7 DAY)),
('CC-004', 'PROD-WHEAT-001',  'Whole Wheat',     'SKU-WHEAT-001',  'EMP-07', 'Priya Nair',    600, 600, DATE_SUB(NOW(), INTERVAL 7 DAY)),
('CC-005', 'PROD-OIL-001',    'Sunflower Oil',   'SKU-OIL-001',    'EMP-02', 'Ankit Sharma',  120, 115, DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE counted_qty = VALUES(counted_qty);

-- ============================================================
-- SUBSYSTEM 4 — UI
-- ============================================================

-- ui_users
INSERT INTO ui_users (username, password_hash, user_role, is_account_locked, login_attempt_count, last_login_timestamp, user_email, user_display_name, theme_preference, language_preference) VALUES
('admin_rajesh',   '$2b$12$abc123hashedpassword01', 'ADMIN',           FALSE, 0, NOW(), 'rajesh@scm.in',    'Rajesh Iyer',      'DARK',  'en'),
('mgr_sunita',     '$2b$12$abc123hashedpassword02', 'MANAGER',         FALSE, 0, NOW(), 'sunita@scm.in',    'Sunita Menon',     'LIGHT', 'en'),
('sales_arun',     '$2b$12$abc123hashedpassword03', 'SALES_REP',       FALSE, 1, NOW(), 'arun@scm.in',      'Arun Pillai',      'LIGHT', 'en'),
('wh_priya',       '$2b$12$abc123hashedpassword04', 'WAREHOUSE_STAFF', FALSE, 0, NOW(), 'priya@scm.in',     'Priya Nair',       'DARK',  'hi'),
('cashier_mona',   '$2b$12$abc123hashedpassword05', 'CASHIER',         FALSE, 2, NOW(), 'mona@scm.in',      'Mona Singh',       'LIGHT', 'en'),
('analyst_deepak', '$2b$12$abc123hashedpassword06', 'ANALYST',         FALSE, 0, NOW(), 'deepak@scm.in',    'Deepak Rao',       'DARK',  'en'),
('driver_rahul',   '$2b$12$abc123hashedpassword07', 'DRIVER',          FALSE, 0, NOW(), 'rahul@scm.in',     'Rahul Verma',      'LIGHT', 'en'),
('mgr_kavitha',    '$2b$12$abc123hashedpassword08', 'MANAGER',         TRUE,  5, NULL,  'kavitha@scm.in',   'Kavitha Reddy',    'LIGHT', 'en')
ON DUPLICATE KEY UPDATE last_login_timestamp = VALUES(last_login_timestamp);

-- ui_sessions
INSERT INTO ui_sessions (user_id, jwt_session_token, redirect_panel_url, session_expiry_time, session_status) VALUES
(1, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin001', '/dashboard',     UNIX_TIMESTAMP(DATE_ADD(NOW(), INTERVAL 8 HOUR)) * 1000, 'ACTIVE'),
(2, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mgr002',   '/orders',        UNIX_TIMESTAMP(DATE_ADD(NOW(), INTERVAL 8 HOUR)) * 1000, 'ACTIVE'),
(3, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sales003', '/pricing',       UNIX_TIMESTAMP(DATE_ADD(NOW(), INTERVAL 4 HOUR)) * 1000, 'ACTIVE'),
(6, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.anly006',  '/analytics',     UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 1 HOUR)) * 1000, 'EXPIRED')
ON DUPLICATE KEY UPDATE session_status = VALUES(session_status);

-- ui_panel_state
INSERT INTO ui_panel_state (user_id, panel_id, current_panel_state) VALUES
(1, 'MAIN_NAV',   'DASHBOARD'),
(2, 'MAIN_NAV',   'ORDERS'),
(3, 'MAIN_NAV',   'PRICING'),
(4, 'MAIN_NAV',   'WAREHOUSE'),
(6, 'MAIN_NAV',   'ANALYTICS'),
(1, 'SIDE_PANEL', 'COLLAPSED'),
(2, 'SIDE_PANEL', 'EXPANDED')
ON DUPLICATE KEY UPDATE current_panel_state = VALUES(current_panel_state);

-- ui_notifications
INSERT INTO ui_notifications (user_id, notification_type, notification_message, is_read) VALUES
(1, 'LOW_STOCK',      'PROD-MILK-001 is below reorder threshold (150 units remaining)',          FALSE),
(2, 'ORDER_ALERT',    'Order ORD-1003 has been stuck in STAGED status for over 2 hours',         FALSE),
(3, 'PROMO_EXPIRY',   'Promo OILDROP12 expires in 5 days — only 95 uses remaining',              TRUE),
(4, 'RETURN_REQUEST', 'New return request RET-001 received for order ORD-1001',                  FALSE),
(6, 'FORECAST_READY', 'Q2 2026 demand forecast for PROD-APPLE-001 generated with 92.5% confidence', TRUE),
(1, 'SYSTEM_ALERT',   'Shipment SHP-001 has MEDIUM-severity congestion alert on planned route',  FALSE),
(2, 'APPROVAL_PEND',  'Price approval APR-002 awaiting your review',                             FALSE)
ON DUPLICATE KEY UPDATE is_read = VALUES(is_read);

-- ui_audit_log
INSERT INTO ui_audit_log (audit_timestamp, audit_action_user, audit_action_description, audit_module_name) VALUES
(NOW(), 'admin_rajesh',   'Updated system config: DEFAULT_CURRENCY set to INR',                   'SystemConfig'),
(NOW(), 'mgr_sunita',     'Approved price override APR-001 for customer CUST-77',                  'PriceApproval'),
(NOW(), 'sales_arun',     'Applied promo code APFEST10 to order ORD-1001',                         'Promotions'),
(NOW(), 'wh_priya',       'Completed cycle count CC-001 — variance of 3 units detected',           'WarehouseOps'),
(NOW(), 'cashier_mona',   'Processed payment for order ORD-1004 — status updated to PAID',         'POS'),
(NOW(), 'analyst_deepak', 'Exported demand forecast report for Q2 2026',                           'Forecasting'),
(NOW(), 'admin_rajesh',   'Locked user account kavitha@scm.in due to repeated failed login attempts','UserManagement')
ON DUPLICATE KEY UPDATE audit_action_description = VALUES(audit_action_description);

-- ui_notification_preferences
INSERT INTO ui_notification_preferences (user_id, pref_key, pref_value) VALUES
(1, 'EMAIL_ON_LOW_STOCK',   TRUE),
(1, 'EMAIL_ON_ORDER_ALERT', TRUE),
(1, 'SMS_ON_SYSTEM_ALERT',  FALSE),
(2, 'EMAIL_ON_LOW_STOCK',   TRUE),
(2, 'EMAIL_ON_ORDER_ALERT', FALSE),
(3, 'EMAIL_ON_PROMO_EXPIRY',TRUE),
(6, 'EMAIL_ON_FORECAST',    TRUE)
ON DUPLICATE KEY UPDATE pref_value = VALUES(pref_value);

-- ui_system_config
INSERT INTO ui_system_config (config_key, config_value) VALUES
('DEFAULT_CURRENCY',       'INR'),
('SESSION_TIMEOUT_MINS',   '480'),
('MAX_LOGIN_ATTEMPTS',     '5'),
('LOW_STOCK_THRESHOLD',    '50'),
('NOTIFICATION_EMAIL_FROM','noreply@scm.in'),
('TIMEZONE',               'Asia/Kolkata'),
('FISCAL_YEAR_START',      '04-01'),
('FORECAST_MODEL_DEFAULT', 'ARIMA')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- ============================================================
-- SUBSYSTEM 5 — DOUBLE-ENTRY STOCK KEEPING
-- ============================================================

INSERT INTO stock_ledger_entries (transaction_id, transaction_type, item_name, quantity, unit, debit_account, credit_account, entry_date, reference_number, total_debit, total_credit, balance_status) VALUES
('TXN-001', 'INBOUND',    'Fresh Apples',    350, 'KG',  'Inventory Asset',   'Supplier Payable',  CURDATE(), 'GR-001', 42000.00, 42000.00, 'BALANCED'),
('TXN-002', 'SALE',       'Fresh Apples',    3,   'KG',  'Cost of Goods Sold','Inventory Asset',   CURDATE(), 'ORD-1001', 360.00, 360.00, 'BALANCED'),
('TXN-003', 'INBOUND',    'Alphonso Mangoes',200, 'KG',  'Inventory Asset',   'Supplier Payable',  CURDATE(), 'GR-002', 11000.00, 11000.00, 'BALANCED'),
('TXN-004', 'SALE',       'Alphonso Mangoes',5,   'KG',  'Cost of Goods Sold','Inventory Asset',   CURDATE(), 'ORD-1002', 275.00,  275.00, 'BALANCED'),
('TXN-005', 'RETURN',     'Fresh Apples',    3,   'KG',  'Inventory Asset',   'Cost of Goods Sold',CURDATE(), 'RET-001',  360.00,  360.00, 'BALANCED'),
('TXN-006', 'ADJUSTMENT', 'Sunflower Oil',   5,   'LITRE','Shrinkage Loss',   'Inventory Asset',   CURDATE(), 'ADJ-001',  500.00,  500.00, 'BALANCED'),
('TXN-007', 'INBOUND',    'Basmati Rice',    2000,'KG',  'Inventory Asset',   'Supplier Payable',  CURDATE(), 'GR-003', 76000.00, 76000.00, 'BALANCED'),
('TXN-008', 'SALE',       'Sunflower Oil',   10,  'LITRE','Cost of Goods Sold','Inventory Asset',  CURDATE(), 'ORD-1005', 1000.00, 1000.00, 'BALANCED')
ON DUPLICATE KEY UPDATE balance_status = VALUES(balance_status);

-- ============================================================
-- SUBSYSTEM — INVENTORY MANAGEMENT
-- ============================================================

-- products
INSERT INTO products (product_id, product_name, sku, category, sub_category, supplier_id, unit_of_measure, storage_conditions, shelf_life_days) VALUES
('PROD-APPLE-001',  'Fresh Apples',       'SKU-APPLE-001',  'Fruits',      'Fresh Fruits', 'SUP-01', 'KG',    'Cool & dry, 4-8°C', 14),
('PROD-MANGO-001',  'Alphonso Mangoes',   'SKU-MANGO-001',  'Fruits',      'Fresh Fruits', 'SUP-02', 'KG',    'Cool & dry, 8-12°C',10),
('PROD-RICE-001',   'Basmati Rice',       'SKU-RICE-001',   'Grains',      'Rice',         'SUP-03', 'KG',    'Dry, below 25°C',   365),
('PROD-WHEAT-001',  'Whole Wheat Flour',  'SKU-WHEAT-001',  'Grains',      'Flour',        'SUP-03', 'KG',    'Dry, below 25°C',   180),
('PROD-MILK-001',   'Full Cream Milk',    'SKU-MILK-001',   'Dairy',       'Milk',         'SUP-04', 'LITRE', 'Refrigerated, 2-4°C',3),
('PROD-SUGAR-001',  'Refined Sugar',      'SKU-SUGAR-001',  'Condiments',  'Sugar',        'SUP-05', 'KG',    'Dry, below 30°C',   730),
('PROD-OIL-001',    'Sunflower Oil',      'SKU-OIL-001',    'Oils & Fats', 'Cooking Oil',  'SUP-05', 'LITRE', 'Cool & dark',       540),
('PROD-BANANA-001', 'Cavendish Bananas',  'SKU-BANANA-001', 'Fruits',      'Fresh Fruits', 'SUP-02', 'KG',    'Ambient, 18-22°C',   7),
('PROD-POTATO-001', 'Fresh Potatoes',     'SKU-POTATO-001', 'Vegetables',  'Root Veg',     'SUP-06', 'KG',    'Cool & dark, 8-12°C',21)
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name);

-- product_batches
INSERT INTO product_batches (batch_id, product_id, lot_id, manufacturing_date, supplier_id, batch_status, received_date) VALUES
('BATCH-001', 'PROD-APPLE-001',  'LOT-A101', DATE_SUB(CURDATE(), INTERVAL 2 DAY),  'SUP-01', 'ACTIVE', NOW()),
('BATCH-002', 'PROD-MANGO-001',  'LOT-M201', DATE_SUB(CURDATE(), INTERVAL 1 DAY),  'SUP-02', 'ACTIVE', NOW()),
('BATCH-003', 'PROD-RICE-001',   'LOT-R301', DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'SUP-03', 'ACTIVE', NOW()),
('BATCH-004', 'PROD-WHEAT-001',  'LOT-W401', DATE_SUB(CURDATE(), INTERVAL 15 DAY), 'SUP-03', 'ACTIVE', NOW()),
('BATCH-005', 'PROD-MILK-001',   'LOT-MK01', CURDATE(),                            'SUP-04', 'ACTIVE', NOW()),
('BATCH-006', 'PROD-OIL-001',    'LOT-O601', DATE_SUB(CURDATE(), INTERVAL 60 DAY), 'SUP-05', 'ACTIVE', NOW()),
('BATCH-007', 'PROD-BANANA-001', 'LOT-B701', CURDATE(),                            'SUP-02', 'ACTIVE', NOW()),
('BATCH-008', 'PROD-APPLE-001',  'LOT-A102', DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'SUP-01', 'BLOCKED',NOW())
ON DUPLICATE KEY UPDATE batch_status = VALUES(batch_status);

-- expiry_tracking
INSERT INTO expiry_tracking (expiry_id, batch_id, expiry_date, days_remaining, expiry_status, alert_flag) VALUES
('EXP-001', 'BATCH-001', DATE_ADD(CURDATE(), INTERVAL 12 DAY), 12, 'VALID',    FALSE),
('EXP-002', 'BATCH-002', DATE_ADD(CURDATE(), INTERVAL 9 DAY),  9,  'VALID',    FALSE),
('EXP-003', 'BATCH-003', DATE_ADD(CURDATE(), INTERVAL 335 DAY),335,'VALID',    FALSE),
('EXP-004', 'BATCH-004', DATE_ADD(CURDATE(), INTERVAL 165 DAY),165,'VALID',    FALSE),
('EXP-005', 'BATCH-005', DATE_ADD(CURDATE(), INTERVAL 3 DAY),  3,  'EXPIRING', TRUE),
('EXP-006', 'BATCH-006', DATE_ADD(CURDATE(), INTERVAL 480 DAY),480,'VALID',    FALSE),
('EXP-007', 'BATCH-007', DATE_ADD(CURDATE(), INTERVAL 7 DAY),  7,  'EXPIRING', TRUE),
('EXP-008', 'BATCH-008', DATE_SUB(CURDATE(), INTERVAL 16 DAY), -16,'EXPIRED',  TRUE)
ON DUPLICATE KEY UPDATE expiry_status = VALUES(expiry_status);

-- stock_levels
INSERT INTO stock_levels (stock_level_id, product_id, current_stock_qty, reserved_stock_qty, available_stock_qty, reorder_threshold, reorder_quantity, safety_stock_level) VALUES
('SL-001', 'PROD-APPLE-001',  350, 3,  347, 100, 200, 50),
('SL-002', 'PROD-MANGO-001',  200, 5,  195, 80,  150, 30),
('SL-003', 'PROD-RICE-001',   800, 20, 780, 200, 500, 100),
('SL-004', 'PROD-WHEAT-001',  600, 0,  600, 150, 400, 80),
('SL-005', 'PROD-MILK-001',   150, 2,  148, 200, 300, 100),
('SL-006', 'PROD-OIL-001',    120, 10, 110, 100, 200, 50),
('SL-007', 'PROD-BANANA-001', 80,  8,  72,  100, 200, 40),
('SL-008', 'PROD-POTATO-001', 300, 0,  300, 120, 250, 60)
ON DUPLICATE KEY UPDATE current_stock_qty = VALUES(current_stock_qty);

-- stock_adjustments
INSERT INTO stock_adjustments (adjustment_id, product_id, batch_id, adjustment_type, quantity_adjusted, reason, adjusted_by) VALUES
('ADJ-001', 'PROD-OIL-001',    'BATCH-006', 'DECREASE',    5,   'Spillage during transfer',        'EMP-04'),
('ADJ-002', 'PROD-APPLE-001',  'BATCH-008', 'DECREASE',    10,  'Expired batch — disposed',        'EMP-03'),
('ADJ-003', 'PROD-RICE-001',   NULL,        'INCREASE',    15,  'Recount correction after audit',  'EMP-07'),
('ADJ-004', 'PROD-MILK-001',   'BATCH-005', 'CORRECTION',  2,   'System sync error corrected',     'EMP-04'),
('ADJ-005', 'PROD-BANANA-001', 'BATCH-007', 'DECREASE',    3,   'Damage during loading',           'EMP-02')
ON DUPLICATE KEY UPDATE quantity_adjusted = VALUES(quantity_adjusted);

-- reorder_management
INSERT INTO reorder_management (reorder_id, product_id, current_stock, reorder_threshold, reorder_quantity, supplier_id, reorder_status, last_reorder_date) VALUES
('REO-001', 'PROD-MILK-001',   150, 200, 300, 'SUP-04', 'PENDING', NULL),
('REO-002', 'PROD-BANANA-001', 80,  100, 200, 'SUP-02', 'PENDING', NULL),
('REO-003', 'PROD-OIL-001',    120, 100, 200, 'SUP-05', 'ORDERED', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('REO-004', 'PROD-APPLE-001',  350, 100, 200, 'SUP-01', 'ORDERED', DATE_SUB(NOW(), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE reorder_status = VALUES(reorder_status);

-- stock_reservations
INSERT INTO stock_reservations (reservation_id, product_id, order_id, reserved_qty, reservation_status, reserved_at, expiry_time) VALUES
('RES-001', 'PROD-APPLE-001',  'ORD-1001', 3,  'ACTIVE',   NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR)),
('RES-002', 'PROD-MANGO-001',  'ORD-1002', 5,  'ACTIVE',   NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR)),
('RES-003', 'PROD-RICE-001',   'ORD-1003', 20, 'ACTIVE',   NOW(), DATE_ADD(NOW(), INTERVAL 4 HOUR)),
('RES-004', 'PROD-MILK-001',   'ORD-1004', 2,  'ACTIVE',   NOW(), DATE_ADD(NOW(), INTERVAL 1 HOUR)),
('RES-005', 'PROD-OIL-001',    'ORD-1005', 10, 'RELEASED', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL)
ON DUPLICATE KEY UPDATE reservation_status = VALUES(reservation_status);

-- stock_freeze
INSERT INTO stock_freeze (freeze_id, product_id, batch_id, freeze_status, freeze_reason, frozen_by, frozen_at) VALUES
('FRZ-001', 'PROD-APPLE-001',  'BATCH-008', TRUE,  'Expired batch pending disposal clearance',    'EMP-07', NOW()),
('FRZ-002', 'PROD-BANANA-001', NULL,        FALSE, 'Quality hold lifted after re-inspection',     'EMP-03', DATE_SUB(NOW(), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE freeze_status = VALUES(freeze_status);

-- dead_stock
INSERT INTO dead_stock (dead_stock_id, product_id, last_movement_date, stagnant_days, stagnant_quantity, action_flag) VALUES
('DS-001', 'PROD-SUGAR-001', DATE_SUB(NOW(), INTERVAL 90 DAY), 90, 500, 'CLEARANCE'),
('DS-002', 'PROD-WHEAT-001', DATE_SUB(NOW(), INTERVAL 45 DAY), 45, 200, 'HOLD')
ON DUPLICATE KEY UPDATE stagnant_days = VALUES(stagnant_days);

-- stock_valuation
INSERT INTO stock_valuation (valuation_id, product_id, unit_cost, total_quantity, total_value, reserved_value, valuation_method) VALUES
('VAL-001', 'PROD-APPLE-001',  80.00,  350, 28000.00, 240.00,  'FIFO'),
('VAL-002', 'PROD-MANGO-001',  55.00,  200, 11000.00, 275.00,  'FIFO'),
('VAL-003', 'PROD-RICE-001',   38.00,  800, 30400.00, 760.00,  'AVG'),
('VAL-004', 'PROD-WHEAT-001',  29.00,  600, 17400.00, 0.00,    'AVG'),
('VAL-005', 'PROD-MILK-001',   20.00,  150, 3000.00,  40.00,   'FIFO'),
('VAL-006', 'PROD-OIL-001',   100.00,  120, 12000.00, 1000.00, 'FIFO'),
('VAL-007', 'PROD-BANANA-001', 22.00,  80,  1760.00,  176.00,  'FIFO'),
('VAL-008', 'PROD-POTATO-001', 15.00,  300, 4500.00,  0.00,    'AVG')
ON DUPLICATE KEY UPDATE total_value = VALUES(total_value);

-- ============================================================
-- SUBSYSTEM — ORDERS
-- ============================================================

INSERT INTO orders (order_id, customer_id, order_status, order_date, total_amount, payment_status, sales_channel) VALUES
('ORD-1001', 'CUST-77', 'CONFIRMED',  NOW(), 420.00,  'PAID',    'ONLINE'),
('ORD-1002', 'CUST-12', 'CONFIRMED',  NOW(), 340.00,  'PAID',    'POS'),
('ORD-1003', 'CUST-33', 'PLACED',     NOW(), 880.00,  'PENDING', 'DISTRIBUTOR'),
('ORD-1004', 'CUST-44', 'CONFIRMED',  NOW(), 56.00,   'PAID',    'ONLINE'),
('ORD-1005', 'CUST-55', 'PLACED',     NOW(), 1450.00, 'PENDING', 'DISTRIBUTOR'),
('ORD-1006', 'CUST-66', 'CANCELLED',  DATE_SUB(NOW(), INTERVAL 2 DAY), 280.00, 'REFUNDED', 'ONLINE'),
('ORD-1007', 'CUST-88', 'FULFILLED',  DATE_SUB(NOW(), INTERVAL 5 DAY), 660.00, 'PAID',     'POS'),
('ORD-1008', 'CUST-99', 'CONFIRMED',  NOW(), 105.00,  'PENDING', 'ONLINE')
ON DUPLICATE KEY UPDATE order_status = VALUES(order_status), total_amount = VALUES(total_amount);

-- order_items
INSERT INTO order_items (order_item_id, order_id, product_id, ordered_quantity, unit_price, line_total) VALUES
('ITEM-1001', 'ORD-1001', 'PROD-APPLE-001',  3,  140.00, 420.00),
('ITEM-1002', 'ORD-1002', 'PROD-MANGO-001',  5,  68.00,  340.00),
('ITEM-1003', 'ORD-1003', 'PROD-RICE-001',   20, 44.00,  880.00),
('ITEM-1004', 'ORD-1004', 'PROD-MILK-001',   2,  28.00,  56.00),
('ITEM-1005', 'ORD-1005', 'PROD-OIL-001',    10, 145.00, 1450.00),
('ITEM-1006', 'ORD-1006', 'PROD-BANANA-001', 8,  35.00,  280.00),
('ITEM-1007', 'ORD-1007', 'PROD-APPLE-001',  3,  120.00, 360.00),
('ITEM-1008', 'ORD-1007', 'PROD-MANGO-001',  2,  80.00,  160.00),
('ITEM-1009', 'ORD-1008', 'PROD-BANANA-001', 3,  35.00,  105.00)
ON DUPLICATE KEY UPDATE ordered_quantity = VALUES(ordered_quantity), line_total = VALUES(line_total);

-- ============================================================
-- SUBSYSTEM — DELIVERY ORDERS
-- ============================================================

INSERT INTO delivery_orders (delivery_id, order_id, customer_id, delivery_address, delivery_status, delivery_type, delivery_cost, assigned_agent, warehouse_id, created_at, updated_at) VALUES
('SHIP-001', 'ORD-1001', 'CUST-77', 'Koramangala, Bengaluru, KA',        'PENDING',   'STANDARD', 180.00, 'AGENT-22', 'WH-001', NOW(), NOW()),
('SHIP-002', 'ORD-1002', 'CUST-12', 'Connaught Place, New Delhi',         'DELIVERED', 'EXPRESS',  250.00, 'AGENT-14', 'WH-002', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
('SHIP-003', 'ORD-1003', 'CUST-33', 'Anna Nagar, Chennai, TN',            'IN_TRANSIT','STANDARD', 200.00, 'AGENT-09', 'WH-003', NOW(), NOW()),
('SHIP-004', 'ORD-1004', 'CUST-44', 'Banjara Hills, Hyderabad, TS',       'PENDING',   'STANDARD', 150.00, 'AGENT-11', 'WH-001', NOW(), NOW()),
('SHIP-005', 'ORD-1005', 'CUST-55', 'Nariman Point, Mumbai, MH',          'PENDING',   'EXPRESS',  320.00, 'AGENT-07', 'WH-005', NOW(), NOW()),
('SHIP-006', 'ORD-1007', 'CUST-88', 'Sector 18, Noida, UP',               'DELIVERED', 'STANDARD', 160.00, 'AGENT-05', 'WH-002', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
('SHIP-007', 'ORD-1008', 'CUST-99', 'Whitefield, Bengaluru, KA',          'PENDING',   'SAME_DAY', 220.00, 'AGENT-22', 'WH-001', NOW(), NOW())
ON DUPLICATE KEY UPDATE delivery_status = VALUES(delivery_status), updated_at = VALUES(updated_at);

-- ============================================================
-- SUBSYSTEM — ORDER FULFILLMENT
-- ============================================================

INSERT INTO fulfillment_orders (fulfillment_id, order_id, fulfillment_status, assigned_warehouse, priority_level, created_at) VALUES
('FULF-001', 'ORD-1001', 'DISPATCHED',  'WH-001', 'HIGH',   NOW()),
('FULF-002', 'ORD-1002', 'DELIVERED',   'WH-002', 'MEDIUM', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('FULF-003', 'ORD-1003', 'PROCESSING',  'WH-003', 'HIGH',   NOW()),
('FULF-004', 'ORD-1004', 'PENDING',     'WH-001', 'LOW',    NOW()),
('FULF-005', 'ORD-1005', 'PENDING',     'WH-005', 'HIGH',   NOW()),
('FULF-006', 'ORD-1007', 'DELIVERED',   'WH-002', 'MEDIUM', DATE_SUB(NOW(), INTERVAL 5 DAY)),
('FULF-007', 'ORD-1008', 'PENDING',     'WH-001', 'MEDIUM', NOW())
ON DUPLICATE KEY UPDATE fulfillment_status = VALUES(fulfillment_status);

INSERT INTO packing_details (packing_id, fulfillment_id, package_type, packed_by, packed_at, package_weight) VALUES
('PACK-001', 'FULF-001', 'CARTON',      'EMP-01', NOW(), 3.50),
('PACK-002', 'FULF-002', 'CARTON',      'EMP-02', DATE_SUB(NOW(), INTERVAL 1 DAY), 5.80),
('PACK-003', 'FULF-003', 'PALLET',      'EMP-03', NOW(), 21.00),
('PACK-004', 'FULF-006', 'CARTON',      'EMP-05', DATE_SUB(NOW(), INTERVAL 5 DAY), 5.00)
ON DUPLICATE KEY UPDATE package_weight = VALUES(package_weight);

-- ============================================================
-- SUBSYSTEM — MULTI-TIER COMMISSION TRACKING
-- ============================================================

INSERT INTO agents (agent_id, agent_name, level, parent_agent_id, status) VALUES
('AGENT-01', 'Suresh Babu',   1, NULL,       'ACTIVE'),
('AGENT-02', 'Deepa Thomas',  2, 'AGENT-01', 'ACTIVE'),
('AGENT-03', 'Mohan Lal',     2, 'AGENT-01', 'ACTIVE'),
('AGENT-04', 'Nisha Kapoor',  3, 'AGENT-02', 'ACTIVE'),
('AGENT-05', 'Rajan Pillai',  3, 'AGENT-02', 'INACTIVE'),
('AGENT-06', 'Seema Gupta',   3, 'AGENT-03', 'ACTIVE')
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO commission_tiers (tier_id, tier_level, min_sales, max_sales, commission_pct) VALUES
('CTIER-01', 1, 0.00,      50000.00,  2.00),
('CTIER-02', 1, 50000.01,  150000.00, 3.50),
('CTIER-03', 1, 150000.01, NULL,      5.00),
('CTIER-04', 2, 0.00,      30000.00,  1.00),
('CTIER-05', 2, 30000.01,  NULL,      1.75),
('CTIER-06', 3, 0.00,      NULL,      0.50)
ON DUPLICATE KEY UPDATE commission_pct = VALUES(commission_pct);

INSERT INTO commission_sales (sale_id, agent_id, sale_amount, sale_date, status) VALUES
('CSALE-001', 'AGENT-02', 12500.00, NOW(), 'CONFIRMED'),
('CSALE-002', 'AGENT-03', 8800.00,  NOW(), 'CONFIRMED'),
('CSALE-003', 'AGENT-04', 4200.00,  NOW(), 'CONFIRMED'),
('CSALE-004', 'AGENT-06', 3400.00,  NOW(), 'CONFIRMED'),
('CSALE-005', 'AGENT-02', 9100.00,  DATE_SUB(NOW(), INTERVAL 5 DAY), 'SETTLED'),
('CSALE-006', 'AGENT-01', 56000.00, DATE_SUB(NOW(), INTERVAL 7 DAY), 'SETTLED')
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO commission_history (commission_id, agent_id, period_start, period_end, total_sales, total_commission, calculated_at) VALUES
('COMM-001', 'AGENT-01', '2026-01-01', '2026-03-31', 210000.00, 10500.00, NOW()),
('COMM-002', 'AGENT-02', '2026-01-01', '2026-03-31', 95000.00,  3325.00,  NOW()),
('COMM-003', 'AGENT-03', '2026-01-01', '2026-03-31', 72000.00,  2520.00,  NOW()),
('COMM-004', 'AGENT-04', '2026-01-01', '2026-03-31', 38000.00,  665.00,   NOW()),
('COMM-005', 'AGENT-06', '2026-01-01', '2026-03-31', 27000.00,  135.00,   NOW())
ON DUPLICATE KEY UPDATE total_commission = VALUES(total_commission);

-- ============================================================
-- SUBSYSTEM — REAL-TIME DELIVERY MONITORING (schema-extension)
-- ============================================================

INSERT INTO delivery_tracking_routes (route_plan_id, delivery_id, carrier_id, tracking_api_url, planned_departure, planned_arrival, current_eta, route_status) VALUES
('DR-001', 'SHIP-001', 'CARRIER-01', 'https://tracking.example/api/SHIP-001', NOW(), DATE_ADD(NOW(), INTERVAL 8 HOUR),  DATE_ADD(NOW(), INTERVAL 9 HOUR),  'IN_PROGRESS'),
('DR-002', 'SHIP-003', 'CARRIER-02', 'https://tracking.example/api/SHIP-003', NOW(), DATE_ADD(NOW(), INTERVAL 12 HOUR), DATE_ADD(NOW(), INTERVAL 13 HOUR), 'IN_PROGRESS'),
('DR-003', 'SHIP-004', 'CARRIER-01', 'https://tracking.example/api/SHIP-004', DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 10 HOUR), NULL, 'PLANNED'),
('DR-004', 'SHIP-005', 'CARRIER-03', 'https://tracking.example/api/SHIP-005', DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 7 HOUR),  NULL, 'PLANNED'),
('DR-005', 'SHIP-007', 'CARRIER-01', 'https://tracking.example/api/SHIP-007', NOW(), DATE_ADD(NOW(), INTERVAL 3 HOUR),  DATE_ADD(NOW(), INTERVAL 3 HOUR),  'IN_PROGRESS')
ON DUPLICATE KEY UPDATE current_eta = VALUES(current_eta), route_status = VALUES(route_status);

INSERT INTO delivery_tracking_waypoints (waypoint_id, route_plan_id, waypoint_sequence, waypoint_location) VALUES
('WP-001', 'DR-001', 1, 'WH-001 Exit Gate, Peenya, Bengaluru'),
('WP-002', 'DR-001', 2, 'Toll Plaza NH-48, Nelamangala'),
('WP-003', 'DR-001', 3, 'Koramangala 4th Block, Bengaluru'),
('WP-004', 'DR-002', 1, 'WH-003 Dispatch Bay, Ambattur, Chennai'),
('WP-005', 'DR-002', 2, 'Anna Nagar West, Chennai'),
('WP-006', 'DR-003', 1, 'WH-001 Staging Area, Peenya, Bengaluru'),
('WP-007', 'DR-003', 2, 'Outer Ring Road, Banjara Hills, Hyderabad'),
('WP-008', 'DR-005', 1, 'WH-001 Exit Gate, Peenya, Bengaluru'),
('WP-009', 'DR-005', 2, 'Whitefield Main Road, Bengaluru')
ON DUPLICATE KEY UPDATE waypoint_location = VALUES(waypoint_location);

INSERT INTO delivery_tracking_events (tracking_event_id, delivery_id, rider_id, vehicle_id, timeline_stage, gps_coordinates, event_timestamp, alert_message, requires_rerouting) VALUES
('DTE-001', 'SHIP-001', 'RIDER-01', 'VEH-01', 'DISPATCHED',    '13.0300,77.5700', NOW(), NULL, FALSE),
('DTE-002', 'SHIP-001', 'RIDER-01', 'VEH-01', 'EN_ROUTE',      '12.9920,77.5910', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NULL, FALSE),
('DTE-003', 'SHIP-003', 'RIDER-04', 'VEH-04', 'DISPATCHED',    '13.0843,80.1778', NOW(), NULL, FALSE),
('DTE-004', 'SHIP-003', 'RIDER-04', 'VEH-04', 'EN_ROUTE',      '13.0869,80.2088', DATE_ADD(NOW(), INTERVAL 20 MINUTE), 'Heavy traffic on GST Road', TRUE),
('DTE-005', 'SHIP-007', 'RIDER-01', 'VEH-02', 'DISPATCHED',    '13.0300,77.5700', NOW(), NULL, FALSE)
ON DUPLICATE KEY UPDATE timeline_stage = VALUES(timeline_stage), gps_coordinates = VALUES(gps_coordinates);

-- ============================================================
-- SUBSYSTEM — TRANSPORT & LOGISTICS (schema-extension)
-- ============================================================

INSERT INTO shipments (shipment_id, order_id, origin_address, destination_address, package_weight, is_drop_ship, shipping_priority, shipment_status, supplier_id, inventory_level, route_id, carrier_id, tracking_id, min_cost_constraint, min_time_constraint, avoid_tolls_constraint, calculated_cost) VALUES
('SHP-001', 'ORD-1001', 'WH-001, Peenya Industrial Area, Bengaluru', 'Koramangala, Bengaluru, KA',         3.50,  FALSE, 'HIGH',   'IN_TRANSIT',  'SUP-01', 350, 'ROUTE-001', 'CARRIER-01', 'TRK-001', FALSE, TRUE,  FALSE, 180.00),
('SHP-002', 'ORD-1002', 'WH-002, Narela Industrial Zone, Delhi',     'Connaught Place, New Delhi',          5.80,  FALSE, 'MEDIUM', 'DELIVERED',   'SUP-02', 200, 'ROUTE-002', 'CARRIER-02', 'TRK-002', TRUE,  FALSE, FALSE, 250.00),
('SHP-003', 'ORD-1003', 'WH-003, Ambattur Industrial Estate, Chennai','Anna Nagar, Chennai, TN',           21.00, FALSE, 'HIGH',   'IN_TRANSIT',  'SUP-03', 800, 'ROUTE-003', 'CARRIER-02', 'TRK-003', FALSE, TRUE,  FALSE, 200.00),
('SHP-004', 'ORD-1004', 'WH-001, Peenya Industrial Area, Bengaluru', 'Banjara Hills, Hyderabad, TS',       2.10,  FALSE, 'LOW',    'PENDING',     'SUP-04', 150, NULL,        'CARRIER-01', 'TRK-004', TRUE,  FALSE, TRUE,  150.00),
('SHP-005', 'ORD-1005', 'WH-005, Bhiwandi Cold Chain Hub, Mumbai',   'Nariman Point, Mumbai, MH',          12.80, FALSE, 'HIGH',   'PENDING',     'SUP-05', 120, NULL,        'CARRIER-03', 'TRK-005', FALSE, TRUE,  FALSE, 320.00),
('SHP-006', 'ORD-1007', 'WH-002, Narela Industrial Zone, Delhi',     'Sector 18, Noida, UP',               5.00,  FALSE, 'MEDIUM', 'DELIVERED',   'SUP-01', 350, 'ROUTE-006', 'CARRIER-01', 'TRK-006', FALSE, FALSE, FALSE, 160.00),
('SHP-007', 'ORD-1008', 'WH-001, Peenya Industrial Area, Bengaluru', 'Whitefield, Bengaluru, KA',          3.20,  FALSE, 'HIGH',   'IN_TRANSIT',  'SUP-02', 80,  'ROUTE-007', 'CARRIER-01', 'TRK-007', FALSE, TRUE,  FALSE, 220.00)
ON DUPLICATE KEY UPDATE shipment_status = VALUES(shipment_status), calculated_cost = VALUES(calculated_cost);

INSERT INTO logistics_routes (route_id, shipment_id, current_eta, timeline_stage, route_status, requires_rerouting) VALUES
('ROUTE-001', 'SHP-001', DATE_ADD(NOW(), INTERVAL 6 HOUR),  'EN_ROUTE',   'ACTIVE',    FALSE),
('ROUTE-002', 'SHP-002', DATE_SUB(NOW(), INTERVAL 2 HOUR),  'DELIVERED',  'COMPLETED', FALSE),
('ROUTE-003', 'SHP-003', DATE_ADD(NOW(), INTERVAL 10 HOUR), 'EN_ROUTE',   'ACTIVE',    TRUE),
('ROUTE-006', 'SHP-006', DATE_SUB(NOW(), INTERVAL 4 DAY),   'DELIVERED',  'COMPLETED', FALSE),
('ROUTE-007', 'SHP-007', DATE_ADD(NOW(), INTERVAL 2 HOUR),  'EN_ROUTE',   'ACTIVE',    FALSE)
ON DUPLICATE KEY UPDATE route_status = VALUES(route_status), current_eta = VALUES(current_eta);

INSERT INTO shipment_alerts (alert_id, shipment_id, alert_message, alert_severity) VALUES
('ALERT-001', 'SHP-001', 'Traffic congestion detected on NH-48 near Nelamangala toll',   'MEDIUM'),
('ALERT-002', 'SHP-003', 'Rerouting required — GST Road blocked due to road works',       'HIGH'),
('ALERT-003', 'SHP-005', 'Carrier confirmation pending — dispatch delayed by 45 minutes', 'LOW'),
('ALERT-004', 'SHP-007', 'Same-day delivery SLA at risk — ETA 2h 50m, threshold 3h',     'MEDIUM')
ON DUPLICATE KEY UPDATE alert_message = VALUES(alert_message);

-- ============================================================
-- SUBSYSTEM — PACKAGING, REPAIRS & RECEIPT MANAGEMENT
-- ============================================================

INSERT INTO packaging_jobs (package_id, order_id, quantity, total_amount, discounts, packaging_status, packed_by) VALUES
('PKG-001', 'ORD-1001', 3,  420.00, 0.00,  'PACKED',     'EMP-01'),
('PKG-002', 'ORD-1002', 5,  340.00, 51.00, 'PACKED',     'EMP-02'),
('PKG-003', 'ORD-1003', 20, 880.00, 0.00,  'IN_PROGRESS','EMP-03'),
('PKG-004', 'ORD-1004', 2,  56.00,  2.80,  'PENDING',    NULL),
('PKG-005', 'ORD-1005', 10, 1450.00,0.00,  'PENDING',    NULL),
('PKG-006', 'ORD-1007', 5,  520.00, 26.00, 'PACKED',     'EMP-05'),
('PKG-007', 'ORD-1008', 3,  105.00, 0.00,  'PENDING',    NULL)
ON DUPLICATE KEY UPDATE packaging_status = VALUES(packaging_status);

INSERT INTO repair_requests (request_id, order_id, product_id, defect_details, request_status) VALUES
('REP-001', 'ORD-1001', 'PROD-APPLE-001', 'Minor packaging tear observed on outer carton',             'OPEN'),
('REP-002', 'ORD-1002', 'PROD-MANGO-001', 'Two mangoes bruised — packaging insufficient for transport', 'UNDER_REVIEW'),
('REP-003', 'ORD-1007', 'PROD-APPLE-001', 'Customer reported dented packaging on delivery',            'RESOLVED')
ON DUPLICATE KEY UPDATE request_status = VALUES(request_status);

INSERT INTO receipt_records (receipt_record_id, order_id, package_id, received_amount, receipt_status) VALUES
('REC-001', 'ORD-1001', 'PKG-001', 420.00, 'RECEIVED'),
('REC-002', 'ORD-1002', 'PKG-002', 289.00, 'RECEIVED'),
('REC-003', 'ORD-1007', 'PKG-006', 494.00, 'RECEIVED'),
('REC-004', 'ORD-1004', NULL,      56.00,  'PENDING')
ON DUPLICATE KEY UPDATE receipt_status = VALUES(receipt_status);

-- ============================================================
-- SUBSYSTEM — PRODUCT ADVANCEMENT & RETURNS MANAGEMENT
-- ============================================================

INSERT INTO product_returns (return_request_id, order_id, customer_id, product_details, defect_details, customer_feedback, transport_details, warranty_valid_until, return_status) VALUES
('RET-001', 'ORD-1001', 'CUST-77', 'Fresh Apples — 3 units (3 KG)',     'Bruising observed on 2 of 3 units upon delivery',   'Would like replacement, not refund',       'Reverse pickup requested from delivery agent', DATE_ADD(NOW(), INTERVAL 30 DAY), 'INITIATED'),
('RET-002', 'ORD-1002', 'CUST-12', 'Alphonso Mangoes — 5 units (5 KG)', 'Overripe — not fit for consumption',                'Requested full refund',                    'Customer will drop off at nearest store',      DATE_ADD(NOW(), INTERVAL 14 DAY), 'APPROVED'),
('RET-003', 'ORD-1007', 'CUST-88', 'Fresh Apples — 1 unit',             'Dented packaging, product inside appears intact',   'Packaging quality needs improvement',      'Resolved in-store',                           DATE_ADD(NOW(), INTERVAL 25 DAY), 'RESOLVED')
ON DUPLICATE KEY UPDATE return_status = VALUES(return_status);

INSERT INTO return_growth_statistics (growth_stat_id, return_request_id, metric_period, return_rate, resolution_rate) VALUES
('RGS-001', 'RET-001', '2026-Q2', 4.50, 92.00),
('RGS-002', 'RET-002', '2026-Q2', 4.50, 92.00),
('RGS-003', 'RET-003', '2026-Q1', 3.80, 95.50)
ON DUPLICATE KEY UPDATE return_rate = VALUES(return_rate), resolution_rate = VALUES(resolution_rate);

-- ============================================================
-- SUBSYSTEM — DEMAND FORECASTING
-- ============================================================

INSERT INTO sales_records (sale_id, product_id, store_id, sale_date, quantity_sold, unit_price, revenue, region) VALUES
('SALE-001', 'PROD-APPLE-001',  'STORE-01', CURDATE(), 10,  140.00, 1400.00,  'SOUTH'),
('SALE-002', 'PROD-MANGO-001',  'STORE-01', CURDATE(), 8,   80.00,  640.00,   'SOUTH'),
('SALE-003', 'PROD-RICE-001',   'STORE-02', CURDATE(), 50,  55.00,  2750.00,  'NORTH'),
('SALE-004', 'PROD-WHEAT-001',  'STORE-02', CURDATE(), 30,  42.00,  1260.00,  'NORTH'),
('SALE-005', 'PROD-MILK-001',   'STORE-03', CURDATE(), 25,  28.00,  700.00,   'WEST'),
('SALE-006', 'PROD-OIL-001',    'STORE-03', CURDATE(), 12,  145.00, 1740.00,  'WEST'),
('SALE-007', 'PROD-BANANA-001', 'STORE-01', CURDATE(), 20,  35.00,  700.00,   'SOUTH'),
('SALE-008', 'PROD-POTATO-001', 'STORE-04', CURDATE(), 40,  22.00,  880.00,   'EAST'),
('SALE-009', 'PROD-APPLE-001',  'STORE-02', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 15, 140.00, 2100.00, 'NORTH'),
('SALE-010', 'PROD-MANGO-001',  'STORE-04', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 6,  80.00,  480.00,  'EAST')
ON DUPLICATE KEY UPDATE quantity_sold = VALUES(quantity_sold), revenue = VALUES(revenue);

INSERT INTO holiday_calendar (holiday_id, holiday_date, holiday_name, holiday_type, region_applicable) VALUES
('HOL-001', CURDATE(),                            'Regional Festival',        'PUBLIC',  'SOUTH'),
('HOL-002', DATE_ADD(CURDATE(), INTERVAL 7 DAY),  'Eid al-Fitr',             'NATIONAL','ALL'),
('HOL-003', DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'Dr. Ambedkar Jayanti',    'PUBLIC',  'NATIONAL'),
('HOL-004', DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'Maharashtra Day',         'REGIONAL','WEST'),
('HOL-005', DATE_ADD(CURDATE(), INTERVAL 60 DAY), 'Ram Navami',              'PUBLIC',  'NORTH'),
('HOL-006', DATE_ADD(CURDATE(), INTERVAL 90 DAY), 'Independence Day',        'NATIONAL','ALL')
ON DUPLICATE KEY UPDATE holiday_name = VALUES(holiday_name);

INSERT INTO promotional_calendar (promo_calendar_id, promo_id, promo_name, promo_start_date, promo_end_date, discount_percentage, promo_type, applicable_products) VALUES
('PC-001', 'PROMO-APPLE',  'Apple Fest',        CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY),  10.00, 'SEASONAL',  'PROD-APPLE-001'),
('PC-002', 'PROMO-MANGO',  'Mango Season Sale', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 15.00, 'SEASONAL',  'PROD-MANGO-001'),
('PC-003', 'PROMO-GRAIN',  'Grain Bonanza',     CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 8.00,  'VOLUME',    'PROD-RICE-001,PROD-WHEAT-001'),
('PC-004', 'PROMO-DAIRY',  'Dairy Delight',     CURDATE(), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 5.00,  'CLEARANCE', 'PROD-MILK-001'),
('PC-005', 'PROMO-OIL',    'Oil Price Drop',    CURDATE(), DATE_ADD(CURDATE(), INTERVAL 5 DAY),  8.00,  'FLASH',     'PROD-OIL-001')
ON DUPLICATE KEY UPDATE discount_percentage = VALUES(discount_percentage);

INSERT INTO product_lifecycle_stages (lifecycle_id, product_id, current_stage, stage_start_date, previous_stage, transition_date) VALUES
('LC-001', 'PROD-APPLE-001',  'GROWTH',        CURDATE(), 'INTRODUCTION', CURDATE()),
('LC-002', 'PROD-MANGO-001',  'MATURITY',      DATE_SUB(CURDATE(), INTERVAL 180 DAY), 'GROWTH', DATE_SUB(CURDATE(), INTERVAL 180 DAY)),
('LC-003', 'PROD-RICE-001',   'MATURITY',      DATE_SUB(CURDATE(), INTERVAL 365 DAY), 'GROWTH', DATE_SUB(CURDATE(), INTERVAL 365 DAY)),
('LC-004', 'PROD-WHEAT-001',  'MATURITY',      DATE_SUB(CURDATE(), INTERVAL 365 DAY), 'GROWTH', DATE_SUB(CURDATE(), INTERVAL 365 DAY)),
('LC-005', 'PROD-MILK-001',   'MATURITY',      DATE_SUB(CURDATE(), INTERVAL 730 DAY), 'GROWTH', DATE_SUB(CURDATE(), INTERVAL 730 DAY)),
('LC-006', 'PROD-BANANA-001', 'INTRODUCTION',  CURDATE(), NULL, NULL),
('LC-007', 'PROD-OIL-001',    'MATURITY',      DATE_SUB(CURDATE(), INTERVAL 500 DAY), 'GROWTH', DATE_SUB(CURDATE(), INTERVAL 500 DAY)),
('LC-008', 'PROD-POTATO-001', 'DECLINE',       DATE_SUB(CURDATE(), INTERVAL 90 DAY),  'MATURITY',DATE_SUB(CURDATE(), INTERVAL 90 DAY))
ON DUPLICATE KEY UPDATE current_stage = VALUES(current_stage);

INSERT INTO demand_forecasts (forecast_id, product_id, forecast_period, forecast_date, predicted_demand, confidence_score, reorder_signal, suggested_order_qty, lifecycle_stage, algorithm_used, generated_at, source_event_reference) VALUES
('DF-001', 'PROD-APPLE-001',  '2026-Q2', CURDATE(), 850,  92.50, FALSE, 200, 'GROWTH',       'ARIMA',   NOW(), 'BAR-001'),
('DF-002', 'PROD-MANGO-001',  '2026-Q2', CURDATE(), 600,  88.00, FALSE, 150, 'MATURITY',     'ARIMA',   NOW(), NULL),
('DF-003', 'PROD-RICE-001',   '2026-Q2', CURDATE(), 3200, 95.00, FALSE, 500, 'MATURITY',     'LSTM',    NOW(), NULL),
('DF-004', 'PROD-WHEAT-001',  '2026-Q2', CURDATE(), 2500, 91.00, FALSE, 400, 'MATURITY',     'LSTM',    NOW(), NULL),
('DF-005', 'PROD-MILK-001',   '2026-Q2', CURDATE(), 1800, 78.50, TRUE,  300, 'MATURITY',     'HOLT_ES', NOW(), NULL),
('DF-006', 'PROD-OIL-001',    '2026-Q2', CURDATE(), 900,  85.00, FALSE, 200, 'MATURITY',     'ARIMA',   NOW(), NULL),
('DF-007', 'PROD-BANANA-001', '2026-Q2', CURDATE(), 450,  70.00, TRUE,  200, 'INTRODUCTION', 'NAIVE',   NOW(), NULL),
('DF-008', 'PROD-POTATO-001', '2026-Q2', CURDATE(), 1100, 82.00, FALSE, 250, 'DECLINE',      'ARIMA',   NOW(), NULL)
ON DUPLICATE KEY UPDATE predicted_demand = VALUES(predicted_demand), confidence_score = VALUES(confidence_score);

INSERT INTO forecast_performance_metrics (eval_id, forecast_id, forecast_date, predicted_qty, actual_qty, mape, rmse, model_used) VALUES
('FPM-001', 'DF-001', CURDATE(), 850,  820,  3.66, 12.4200, 'ARIMA'),
('FPM-002', 'DF-002', CURDATE(), 600,  580,  3.45, 11.8000, 'ARIMA'),
('FPM-003', 'DF-003', CURDATE(), 3200, 3150, 1.59, 35.3600, 'LSTM'),
('FPM-004', 'DF-004', CURDATE(), 2500, 2420, 3.31, 56.5700, 'LSTM'),
('FPM-005', 'DF-005', CURDATE(), 1800, NULL, NULL, NULL,     'HOLT_ES'),
('FPM-006', 'DF-006', CURDATE(), 900,  870,  3.45, 21.2100, 'ARIMA'),
('FPM-007', 'DF-007', CURDATE(), 450,  NULL, NULL, NULL,     'NAIVE'),
('FPM-008', 'DF-008', CURDATE(), 1100, 1080, 1.85, 14.1400, 'ARIMA')
ON DUPLICATE KEY UPDATE actual_qty = VALUES(actual_qty), mape = VALUES(mape), rmse = VALUES(rmse);

-- ============================================================
-- BARCODE / RFID EVENTS & SUBSYSTEM EXCEPTIONS
-- ============================================================

INSERT INTO barcode_rfid_events (event_id, product_id, event_type, source_device, warehouse_id, event_timestamp, raw_payload) VALUES
('BAR-001', 'PROD-APPLE-001',  'SCAN_IN',   'RFID_GATE_A1', 'WH-001', NOW(), '{"tag":"RFID-7788","pallet":"PLT-001"}'),
('BAR-002', 'PROD-MANGO-001',  'SCAN_IN',   'RFID_GATE_A1', 'WH-001', NOW(), '{"tag":"RFID-7789","pallet":"PLT-002"}'),
('BAR-003', 'PROD-APPLE-001',  'SCAN_OUT',  'RFID_GATE_B2', 'WH-001', NOW(), '{"tag":"RFID-7788","order":"ORD-1001"}'),
('BAR-004', 'PROD-RICE-001',   'SCAN_IN',   'RFID_GATE_A1', 'WH-003', DATE_SUB(NOW(), INTERVAL 1 HOUR), '{"tag":"RFID-8001","pallet":"PLT-010"}'),
('BAR-005', 'PROD-OIL-001',    'SCAN_IN',   'BARCODE_HH01', 'WH-005', DATE_SUB(NOW(), INTERVAL 2 HOUR), '{"barcode":"8901234567890","qty":20}'),
('BAR-006', 'PROD-BANANA-001', 'SCAN_IN',   'RFID_GATE_A1', 'WH-001', DATE_SUB(NOW(), INTERVAL 30 MINUTE), '{"tag":"RFID-9001","pallet":"PLT-015"}'),
('BAR-007', 'PROD-MANGO-001',  'SCAN_OUT',  'RFID_GATE_B2', 'WH-001', NOW(), '{"tag":"RFID-7789","order":"ORD-1002"}')
ON DUPLICATE KEY UPDATE raw_payload = VALUES(raw_payload), event_timestamp = VALUES(event_timestamp);

INSERT INTO subsystem_exceptions (exception_id, subsystem_name, reference_id, severity, exception_message, status, created_at, resolved_at) VALUES
('EX-001', 'DeliveryTracking',    'SHIP-001', 'HIGH',   'Shipment delayed due to route reassignment from congestion alert',     'OPEN',     NOW(), NULL),
('EX-002', 'DemandForecasting',   'DF-005',   'MEDIUM', 'Low confidence score (78.5%) — insufficient historical data for Milk', 'OPEN',     NOW(), NULL),
('EX-003', 'WarehouseManagement', 'BATCH-008','HIGH',   'Expired batch BATCH-008 still occupies BIN-H01 — disposal pending',   'OPEN',     NOW(), NULL),
('EX-004', 'PriceApproval',       'APR-002',  'MEDIUM', 'Approval APR-002 pending for >24h — SLA breach risk',                 'OPEN',     NOW(), NULL),
('EX-005', 'OrderFulfillment',    'ORD-1003', 'LOW',    'Fulfillment FULF-003 in PROCESSING state beyond expected duration',   'OPEN',     NOW(), NULL),
('EX-006', 'DeliveryTracking',    'SHIP-003', 'HIGH',   'Rerouting flag raised — GST Road blocked for SHP-003',                'RESOLVED', DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), exception_message = VALUES(exception_message);

SET FOREIGN_KEY_CHECKS = 1;
