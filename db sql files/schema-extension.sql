-- ============================================================
-- SUBSYSTEM — REAL-TIME DELIVERY MONITORING
-- Captures route plans, assignments, and delivery tracking
-- events that go beyond the base delivery_orders table.
-- ============================================================

CREATE TABLE IF NOT EXISTS delivery_tracking_routes (
    route_plan_id                    VARCHAR(50)    NOT NULL,
    delivery_id                      VARCHAR(50)    NOT NULL,
    carrier_id                       VARCHAR(50)    NULL,
    tracking_api_url                 VARCHAR(255)   NULL,
    planned_departure                DATETIME       NULL,
    planned_arrival                  DATETIME       NULL,
    current_eta                      DATETIME       NULL,
    route_status                     VARCHAR(50)    NOT NULL DEFAULT 'PLANNED',

    PRIMARY KEY (route_plan_id),
    FOREIGN KEY (delivery_id) REFERENCES delivery_orders(delivery_id)
        ON DELETE CASCADE
) COMMENT 'Route-plan level tracking for real-time delivery monitoring';


CREATE TABLE IF NOT EXISTS delivery_tracking_waypoints (
    waypoint_id                      VARCHAR(50)    NOT NULL,
    route_plan_id                    VARCHAR(50)    NOT NULL,
    waypoint_sequence                INT            NOT NULL,
    waypoint_location                VARCHAR(255)   NOT NULL,

    PRIMARY KEY (waypoint_id),
    FOREIGN KEY (route_plan_id) REFERENCES delivery_tracking_routes(route_plan_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_waypoint_sequence CHECK (waypoint_sequence > 0)
) COMMENT 'Waypoint list for monitored delivery routes';


CREATE TABLE IF NOT EXISTS delivery_tracking_events (
    tracking_event_id                VARCHAR(50)    NOT NULL,
    delivery_id                      VARCHAR(50)    NOT NULL,
    rider_id                         VARCHAR(50)    NULL,
    vehicle_id                       VARCHAR(50)    NULL,
    timeline_stage                   VARCHAR(50)    NOT NULL,
    gps_coordinates                  VARCHAR(100)   NULL,
    event_timestamp                  DATETIME       NOT NULL,
    alert_message                    VARCHAR(255)   NULL,
    requires_rerouting               BOOLEAN        NOT NULL DEFAULT FALSE,

    PRIMARY KEY (tracking_event_id),
    FOREIGN KEY (delivery_id) REFERENCES delivery_orders(delivery_id)
        ON DELETE CASCADE
) COMMENT 'Live delivery milestones and alert events';


-- ============================================================
-- SUBSYSTEM — TRANSPORT & LOGISTICS / ADVANCED ROUTING
-- Models shipment planning, carrier allocation, and route
-- optimisation data not covered by delivery_orders alone.
-- ============================================================

CREATE TABLE IF NOT EXISTS shipments (
    shipment_id                      VARCHAR(50)    NOT NULL,
    order_id                         VARCHAR(50)    NOT NULL,
    origin_address                   TEXT           NOT NULL,
    destination_address              TEXT           NOT NULL,
    package_weight                   DECIMAL(10,2)  NOT NULL,
    is_drop_ship                     BOOLEAN        NOT NULL DEFAULT FALSE,
    shipping_priority                VARCHAR(50)    NOT NULL,
    shipment_status                  VARCHAR(50)    NOT NULL,
    supplier_id                      VARCHAR(50)    NULL,
    inventory_level                  INT            NULL,
    route_id                         VARCHAR(50)    NULL,
    carrier_id                       VARCHAR(50)    NULL,
    tracking_id                      VARCHAR(50)    NULL,
    min_cost_constraint              BOOLEAN        NOT NULL DEFAULT FALSE,
    min_time_constraint              BOOLEAN        NOT NULL DEFAULT FALSE,
    avoid_tolls_constraint           BOOLEAN        NOT NULL DEFAULT FALSE,
    calculated_cost                  DECIMAL(12,2)  NULL,
    created_at                       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (shipment_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT chk_shipment_weight CHECK (package_weight > 0),
    CONSTRAINT chk_inventory_level_nonneg CHECK (inventory_level IS NULL OR inventory_level >= 0)
) COMMENT 'Shipment planning for transport and logistics decisions';


CREATE TABLE IF NOT EXISTS logistics_routes (
    route_id                         VARCHAR(50)    NOT NULL,
    shipment_id                      VARCHAR(50)    NOT NULL,
    current_eta                      DATETIME       NULL,
    timeline_stage                   VARCHAR(50)    NULL,
    route_status                     VARCHAR(50)    NOT NULL DEFAULT 'PLANNED',
    requires_rerouting               BOOLEAN        NOT NULL DEFAULT FALSE,

    PRIMARY KEY (route_id),
    FOREIGN KEY (shipment_id) REFERENCES shipments(shipment_id)
        ON DELETE CASCADE
) COMMENT 'Calculated logistics routes for shipments';


CREATE TABLE IF NOT EXISTS shipment_alerts (
    alert_id                         VARCHAR(50)    NOT NULL,
    shipment_id                      VARCHAR(50)    NOT NULL,
    alert_message                    VARCHAR(255)   NOT NULL,
    alert_severity                   VARCHAR(20)    NOT NULL,
    created_at                       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (alert_id),
    FOREIGN KEY (shipment_id) REFERENCES shipments(shipment_id)
        ON DELETE CASCADE
) COMMENT 'Alerts produced by transport and logistics flows';


-- ============================================================
-- SUBSYSTEM — PACKAGING, REPAIRS & RECEIPT MANAGEMENT
-- Tracks packaging jobs, repair requests, and receipt-side
-- acknowledgements around order handling.
-- ============================================================

CREATE TABLE IF NOT EXISTS packaging_jobs (
    package_id                       VARCHAR(50)    NOT NULL,
    order_id                         VARCHAR(50)    NOT NULL,
    quantity                         INT            NOT NULL,
    total_amount                     DECIMAL(12,2)  NOT NULL,
    discounts                        DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    packaging_status                 VARCHAR(50)    NOT NULL,
    packed_by                        VARCHAR(50)    NULL,
    created_at                       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (package_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT chk_packaging_qty CHECK (quantity > 0),
    CONSTRAINT chk_packaging_total CHECK (total_amount >= 0),
    CONSTRAINT chk_packaging_discount CHECK (discounts >= 0)
) COMMENT 'Packaging jobs linked to order handling';


CREATE TABLE IF NOT EXISTS repair_requests (
    request_id                       VARCHAR(50)    NOT NULL,
    order_id                         VARCHAR(50)    NOT NULL,
    product_id                       VARCHAR(50)    NOT NULL,
    defect_details                   TEXT           NOT NULL,
    request_status                   VARCHAR(50)    NOT NULL,
    requested_at                     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (request_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
) COMMENT 'Repair requests raised for packaged or delivered items';


CREATE TABLE IF NOT EXISTS receipt_records (
    receipt_record_id                VARCHAR(50)    NOT NULL,
    order_id                         VARCHAR(50)    NOT NULL,
    package_id                       VARCHAR(50)    NULL,
    received_amount                  DECIMAL(12,2)  NOT NULL,
    receipt_status                   VARCHAR(50)    NOT NULL,
    recorded_at                      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (receipt_record_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (package_id) REFERENCES packaging_jobs(package_id),
    CONSTRAINT chk_receipt_amount CHECK (received_amount >= 0)
) COMMENT 'Receipt and acknowledgement records for packaged orders';


-- ============================================================
-- SUBSYSTEM — PRODUCT ADVANCEMENT & RETURNS MANAGEMENT
-- Goes beyond warehouse_returns by storing customer-facing
-- return requests, defect details, and trend metrics.
-- ============================================================

CREATE TABLE IF NOT EXISTS product_returns (
    return_request_id                VARCHAR(50)    NOT NULL,
    order_id                         VARCHAR(50)    NOT NULL,
    customer_id                      VARCHAR(50)    NOT NULL,
    product_details                  TEXT           NOT NULL,
    defect_details                   TEXT           NULL,
    customer_feedback                TEXT           NULL,
    transport_details                TEXT           NULL,
    warranty_valid_until             DATETIME       NULL,
    return_status                    VARCHAR(50)    NOT NULL,
    created_at                       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (return_request_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
) COMMENT 'Customer-facing product return and advancement records';


CREATE TABLE IF NOT EXISTS return_growth_statistics (
    growth_stat_id                   VARCHAR(50)    NOT NULL,
    return_request_id                VARCHAR(50)    NOT NULL,
    metric_period                    VARCHAR(30)    NOT NULL,
    return_rate                      DECIMAL(8,2)   NULL,
    resolution_rate                  DECIMAL(8,2)   NULL,
    recorded_at                      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (growth_stat_id),
    FOREIGN KEY (return_request_id) REFERENCES product_returns(return_request_id)
        ON DELETE CASCADE
) COMMENT 'Trend and growth metrics for returns management';


-- ============================================================
-- SUBSYSTEM — DEMAND FORECASTING (SUPPORT TABLES)
-- The main forecast output table already exists in schema.sql.
-- These tables capture supporting inputs and evaluation data.
-- ============================================================

CREATE TABLE IF NOT EXISTS sales_records (
    sale_id                          VARCHAR(50)    NOT NULL,
    product_id                       VARCHAR(50)    NOT NULL,
    store_id                         VARCHAR(50)    NOT NULL,
    sale_date                        DATE           NOT NULL,
    quantity_sold                    INT            NOT NULL,
    unit_price                       DECIMAL(12,2)  NOT NULL,
    revenue                          DECIMAL(14,2)  NOT NULL,
    region                           VARCHAR(50)    NULL,

    PRIMARY KEY (sale_id),
    CONSTRAINT chk_sales_qty CHECK (quantity_sold >= 0),
    CONSTRAINT chk_sales_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_sales_revenue CHECK (revenue >= 0)
) COMMENT 'Historical sales inputs for forecasting models';


CREATE TABLE IF NOT EXISTS holiday_calendar (
    holiday_id                       VARCHAR(50)    NOT NULL,
    holiday_date                     DATE           NOT NULL,
    holiday_name                     VARCHAR(100)   NOT NULL,
    holiday_type                     VARCHAR(50)    NOT NULL,
    region_applicable                VARCHAR(50)    NULL,

    PRIMARY KEY (holiday_id)
) COMMENT 'Holiday and calendar features for demand forecasting';


CREATE TABLE IF NOT EXISTS promotional_calendar (
    promo_calendar_id                VARCHAR(50)    NOT NULL,
    promo_id                         VARCHAR(50)    NULL,
    promo_name                       VARCHAR(100)   NOT NULL,
    promo_start_date                 DATE           NOT NULL,
    promo_end_date                   DATE           NOT NULL,
    discount_percentage              DECIMAL(5,2)   NULL,
    promo_type                       VARCHAR(50)    NULL,
    applicable_products              TEXT           NULL,

    PRIMARY KEY (promo_calendar_id),
    CONSTRAINT chk_promo_calendar_range CHECK (promo_end_date >= promo_start_date)
) COMMENT 'Promotional events used as demand-forecasting features';


CREATE TABLE IF NOT EXISTS product_lifecycle_stages (
    lifecycle_id                     VARCHAR(50)    NOT NULL,
    product_id                       VARCHAR(50)    NOT NULL,
    current_stage                    VARCHAR(50)    NOT NULL,
    stage_start_date                 DATE           NOT NULL,
    previous_stage                   VARCHAR(50)    NULL,
    transition_date                  DATE           NULL,

    PRIMARY KEY (lifecycle_id)
) COMMENT 'Lifecycle-stage metadata for products in forecasting';


CREATE TABLE IF NOT EXISTS forecast_performance_metrics (
    eval_id                          VARCHAR(50)    NOT NULL,
    forecast_id                      VARCHAR(50)    NOT NULL,
    forecast_date                    DATE           NOT NULL,
    predicted_qty                    INT            NOT NULL,
    actual_qty                       INT            NULL,
    mape                             DECIMAL(8,2)   NULL,
    rmse                             DECIMAL(12,4)  NULL,
    model_used                       VARCHAR(100)   NULL,

    PRIMARY KEY (eval_id),
    FOREIGN KEY (forecast_id) REFERENCES demand_forecasts(forecast_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_predicted_qty_nonneg CHECK (predicted_qty >= 0),
    CONSTRAINT chk_actual_qty_nonneg CHECK (actual_qty IS NULL OR actual_qty >= 0)
) COMMENT 'Evaluation metrics for generated demand forecasts';

CREATE TABLE IF NOT EXISTS barcode_rfid_events (
    event_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    source_device VARCHAR(100) NOT NULL,
    warehouse_id VARCHAR(50) NULL,
    event_timestamp DATETIME NOT NULL,
    raw_payload TEXT NULL,
    PRIMARY KEY (event_id)
);

CREATE TABLE IF NOT EXISTS subsystem_exceptions (
    exception_id VARCHAR(50) NOT NULL,
    subsystem_name VARCHAR(100) NOT NULL,
    reference_id VARCHAR(50) NULL,
    severity VARCHAR(20) NOT NULL,
    exception_message VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    PRIMARY KEY (exception_id)
);

-- ============================================================
-- DELIVERY MONITORING DB MODE COMPATIBILITY
-- Required by DeliveryMonitoringFacadeDB from
-- ramen-noodles-delivery-monitoring.jar
-- ============================================================

-- Existing SCM orders table must be extended so the delivery DB facade
-- can persist delivery-monitoring order state into the same table name.
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS rider_id VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS pickup_address TEXT NULL,
    ADD COLUMN IF NOT EXISTS dropoff_address TEXT NULL,
    ADD COLUMN IF NOT EXISTS pickup_latitude DOUBLE NULL,
    ADD COLUMN IF NOT EXISTS pickup_longitude DOUBLE NULL,
    ADD COLUMN IF NOT EXISTS dropoff_latitude DOUBLE NULL,
    ADD COLUMN IF NOT EXISTS dropoff_longitude DOUBLE NULL,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS riders (
    rider_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS devices (
    device_id VARCHAR(50) PRIMARY KEY,
    rider_id VARCHAR(50) NOT NULL,
    online BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rider_id) REFERENCES riders(rider_id)
);

CREATE TABLE IF NOT EXISTS gps_pings (
    ping_id VARCHAR(50) PRIMARY KEY,
    device_id VARCHAR(50) NOT NULL,
    rider_id VARCHAR(50) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    ping_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(device_id)
);

CREATE TABLE IF NOT EXISTS delivery_status_log (
    log_id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    trigger_source VARCHAR(20) NOT NULL,
    changed_by VARCHAR(50),
    changed_at TIMESTAMP NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE IF NOT EXISTS pod_records (
    pod_id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    rider_id VARCHAR(50) NOT NULL,
    signature_url VARCHAR(255),
    photo_url VARCHAR(255),
    notes TEXT,
    submitted_at TIMESTAMP NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE IF NOT EXISTS notification_logs (
    notification_id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);
