-- ============================================================
-- SCM Delivery Monitoring DB Mode — Dummy Seed Data
-- For tables added in schema-extension.sql (DB mode compatibility)
-- ============================================================

USE OOAD;

-- ============================================================
-- 1) customers
-- ============================================================
INSERT INTO customers (customer_id, name, email, phone, created_at) VALUES
('CUST-77', 'Ananya Rao',      'ananya.rao@customer.in',    '+919900000077', NOW()),
('CUST-12', 'Rohit Mehra',     'rohit.mehra@customer.in',   '+919900000012', NOW()),
('CUST-33', 'Meera Nambiar',   'meera.nambiar@customer.in', '+919900000033', NOW()),
('CUST-44', 'Arjun Bhat',      'arjun.bhat@customer.in',    '+919900000044', NOW()),
('CUST-55', 'Kavya Iyer',      'kavya.iyer@customer.in',    '+919900000055', NOW()),
('CUST-66', 'Sanjay Gupta',    'sanjay.gupta@customer.in',  '+919900000066', NOW()),
('CUST-88', 'Nitin Sharma',    'nitin.sharma@customer.in',  '+919900000088', NOW()),
('CUST-99', 'Pooja Reddy',     'pooja.reddy@customer.in',   '+919900000099', NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
email = VALUES(email),
phone = VALUES(phone);

-- ============================================================
-- 2) riders
-- ============================================================
INSERT INTO riders (rider_id, name, phone, vehicle_type, status, created_at) VALUES
('RIDER-01', 'Rahul Verma',  '+918800000001', 'BIKE', 'ON_DELIVERY', NOW()),
('RIDER-02', 'Amit Das',     '+918800000002', 'BIKE', 'ACTIVE',      NOW()),
('RIDER-03', 'Neha Singh',   '+918800000003', 'SCOOTER', 'ON_BREAK', NOW()),
('RIDER-04', 'Farhan Ali',   '+918800000004', 'BIKE', 'ON_DELIVERY', NOW()),
('RIDER-05', 'Suresh Yadav', '+918800000005', 'VAN',  'ACTIVE',      NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
phone = VALUES(phone),
vehicle_type = VALUES(vehicle_type),
status = VALUES(status);

-- ============================================================
-- 3) devices
-- ============================================================
INSERT INTO devices (device_id, rider_id, online, created_at) VALUES
('DEV-001', 'RIDER-01', TRUE, NOW()),
('DEV-002', 'RIDER-02', TRUE, NOW()),
('DEV-003', 'RIDER-03', TRUE, NOW()),
('DEV-004', 'RIDER-04', TRUE, NOW()),
('DEV-005', 'RIDER-05', TRUE, NOW())
ON DUPLICATE KEY UPDATE
rider_id = VALUES(rider_id),
online = VALUES(online);

-- ============================================================
-- 4) orders (new DB-mode columns only)
--    Uses existing orders seeded in sample-data.sql
-- ============================================================
UPDATE orders
SET
    rider_id = CASE order_id
        WHEN 'ORD-1001' THEN 'RIDER-01'
        WHEN 'ORD-1002' THEN 'RIDER-02'
        WHEN 'ORD-1003' THEN 'RIDER-04'
        WHEN 'ORD-1004' THEN 'RIDER-05'
        WHEN 'ORD-1005' THEN 'RIDER-02'
        WHEN 'ORD-1007' THEN 'RIDER-05'
        WHEN 'ORD-1008' THEN 'RIDER-01'
        ELSE rider_id
    END,
    pickup_address = CASE order_id
        WHEN 'ORD-1001' THEN 'WH-001, Peenya Industrial Area, Bengaluru'
        WHEN 'ORD-1002' THEN 'WH-002, Narela Industrial Zone, Delhi'
        WHEN 'ORD-1003' THEN 'WH-003, Ambattur Industrial Estate, Chennai'
        WHEN 'ORD-1004' THEN 'WH-001, Peenya Industrial Area, Bengaluru'
        WHEN 'ORD-1005' THEN 'WH-005, Bhiwandi Cold Chain Hub, Mumbai'
        WHEN 'ORD-1007' THEN 'WH-002, Narela Industrial Zone, Delhi'
        WHEN 'ORD-1008' THEN 'WH-001, Peenya Industrial Area, Bengaluru'
        ELSE pickup_address
    END,
    dropoff_address = CASE order_id
        WHEN 'ORD-1001' THEN 'Koramangala, Bengaluru, KA'
        WHEN 'ORD-1002' THEN 'Connaught Place, New Delhi'
        WHEN 'ORD-1003' THEN 'Anna Nagar, Chennai, TN'
        WHEN 'ORD-1004' THEN 'Banjara Hills, Hyderabad, TS'
        WHEN 'ORD-1005' THEN 'Nariman Point, Mumbai, MH'
        WHEN 'ORD-1007' THEN 'Sector 18, Noida, UP'
        WHEN 'ORD-1008' THEN 'Whitefield, Bengaluru, KA'
        ELSE dropoff_address
    END,
    pickup_latitude = CASE order_id
        WHEN 'ORD-1001' THEN 13.0329
        WHEN 'ORD-1002' THEN 28.8576
        WHEN 'ORD-1003' THEN 13.1156
        WHEN 'ORD-1004' THEN 13.0329
        WHEN 'ORD-1005' THEN 19.2813
        WHEN 'ORD-1007' THEN 28.8576
        WHEN 'ORD-1008' THEN 13.0329
        ELSE pickup_latitude
    END,
    pickup_longitude = CASE order_id
        WHEN 'ORD-1001' THEN 77.5623
        WHEN 'ORD-1002' THEN 77.0868
        WHEN 'ORD-1003' THEN 80.1500
        WHEN 'ORD-1004' THEN 77.5623
        WHEN 'ORD-1005' THEN 73.0483
        WHEN 'ORD-1007' THEN 77.0868
        WHEN 'ORD-1008' THEN 77.5623
        ELSE pickup_longitude
    END,
    dropoff_latitude = CASE order_id
        WHEN 'ORD-1001' THEN 12.9352
        WHEN 'ORD-1002' THEN 28.6315
        WHEN 'ORD-1003' THEN 13.0849
        WHEN 'ORD-1004' THEN 17.4126
        WHEN 'ORD-1005' THEN 18.9256
        WHEN 'ORD-1007' THEN 28.5708
        WHEN 'ORD-1008' THEN 12.9698
        ELSE dropoff_latitude
    END,
    dropoff_longitude = CASE order_id
        WHEN 'ORD-1001' THEN 77.6245
        WHEN 'ORD-1002' THEN 77.2167
        WHEN 'ORD-1003' THEN 80.2088
        WHEN 'ORD-1004' THEN 78.4482
        WHEN 'ORD-1005' THEN 72.8242
        WHEN 'ORD-1007' THEN 77.3260
        WHEN 'ORD-1008' THEN 77.7500
        ELSE dropoff_longitude
    END,
    status = CASE order_id
        WHEN 'ORD-1001' THEN 'IN_TRANSIT'
        WHEN 'ORD-1002' THEN 'DELIVERED'
        WHEN 'ORD-1003' THEN 'ASSIGNED'
        WHEN 'ORD-1004' THEN 'CREATED'
        WHEN 'ORD-1005' THEN 'CREATED'
        WHEN 'ORD-1006' THEN 'CANCELLED'
        WHEN 'ORD-1007' THEN 'DELIVERED'
        WHEN 'ORD-1008' THEN 'IN_TRANSIT'
        ELSE status
    END,
    created_at = COALESCE(created_at, order_date, NOW())
WHERE order_id IN ('ORD-1001','ORD-1002','ORD-1003','ORD-1004','ORD-1005','ORD-1006','ORD-1007','ORD-1008');

-- ============================================================
-- 5) gps_pings
-- ============================================================
INSERT INTO gps_pings (ping_id, device_id, rider_id, latitude, longitude, ping_timestamp, created_at) VALUES
('PING-001', 'DEV-001', 'RIDER-01', 12.9701, 77.6101, DATE_SUB(NOW(), INTERVAL 25 MINUTE), NOW()),
('PING-002', 'DEV-001', 'RIDER-01', 12.9625, 77.6187, DATE_SUB(NOW(), INTERVAL 15 MINUTE), NOW()),
('PING-003', 'DEV-001', 'RIDER-01', 12.9489, 77.6223, DATE_SUB(NOW(), INTERVAL 5 MINUTE),  NOW()),
('PING-004', 'DEV-002', 'RIDER-02', 28.6509, 77.2300, DATE_SUB(NOW(), INTERVAL 35 MINUTE), NOW()),
('PING-005', 'DEV-004', 'RIDER-04', 13.0890, 80.2012, DATE_SUB(NOW(), INTERVAL 10 MINUTE), NOW()),
('PING-006', 'DEV-005', 'RIDER-05', 17.4200, 78.4560, DATE_SUB(NOW(), INTERVAL 20 MINUTE), NOW())
ON DUPLICATE KEY UPDATE
latitude = VALUES(latitude),
longitude = VALUES(longitude),
ping_timestamp = VALUES(ping_timestamp);

-- ============================================================
-- 6) delivery_status_log
-- ============================================================
INSERT INTO delivery_status_log (log_id, order_id, status, trigger_source, changed_by, changed_at) VALUES
('DSL-001', 'ORD-1001', 'CREATED',    'SYSTEM', 'SYSTEM',    DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
('DSL-002', 'ORD-1001', 'ASSIGNED',   'SYSTEM', 'DISPATCH',  DATE_SUB(NOW(), INTERVAL 70 MINUTE)),
('DSL-003', 'ORD-1001', 'IN_TRANSIT', 'GPS',    'RIDER-01',  DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
('DSL-004', 'ORD-1002', 'CREATED',    'SYSTEM', 'SYSTEM',    DATE_SUB(NOW(), INTERVAL 1 DAY)),
('DSL-005', 'ORD-1002', 'DELIVERED',  'SYSTEM', 'RIDER-02',  DATE_SUB(NOW(), INTERVAL 20 HOUR)),
('DSL-006', 'ORD-1003', 'CREATED',    'SYSTEM', 'SYSTEM',    DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
('DSL-007', 'ORD-1003', 'ASSIGNED',   'SYSTEM', 'DISPATCH',  DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
('DSL-008', 'ORD-1008', 'IN_TRANSIT', 'GPS',    'RIDER-01',  DATE_SUB(NOW(), INTERVAL 10 MINUTE))
ON DUPLICATE KEY UPDATE
status = VALUES(status),
trigger_source = VALUES(trigger_source),
changed_by = VALUES(changed_by),
changed_at = VALUES(changed_at);

-- ============================================================
-- 7) pod_records (delivered orders)
-- ============================================================
INSERT INTO pod_records (pod_id, order_id, rider_id, signature_url, photo_url, notes, submitted_at) VALUES
('POD-001', 'ORD-1002', 'RIDER-02', 'https://cdn.scm.local/pod/signatures/POD-001.png', 'https://cdn.scm.local/pod/photos/POD-001.jpg', 'Delivered to front desk. Receiver: Mr. Khanna.', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
('POD-002', 'ORD-1007', 'RIDER-05', 'https://cdn.scm.local/pod/signatures/POD-002.png', 'https://cdn.scm.local/pod/photos/POD-002.jpg', 'Delivered to customer at gate. OTP verified.',     DATE_SUB(NOW(), INTERVAL 4 DAY))
ON DUPLICATE KEY UPDATE
signature_url = VALUES(signature_url),
photo_url = VALUES(photo_url),
notes = VALUES(notes),
submitted_at = VALUES(submitted_at);

-- ============================================================
-- 8) notification_logs
-- ============================================================
INSERT INTO notification_logs (notification_id, order_id, customer_id, channel, message, status, sent_at) VALUES
('NLOG-001', 'ORD-1001', 'CUST-77', 'SMS',   'Your order ORD-1001 is on the way.',                   'SENT',      DATE_SUB(NOW(), INTERVAL 38 MINUTE)),
('NLOG-002', 'ORD-1001', 'CUST-77', 'PUSH',  'Rider is 20 minutes away for ORD-1001.',               'DELIVERED', DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
('NLOG-003', 'ORD-1002', 'CUST-12', 'EMAIL', 'Order ORD-1002 delivered successfully.',               'DELIVERED', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
('NLOG-004', 'ORD-1003', 'CUST-33', 'SMS',   'Rider assigned for ORD-1003. Track live in app.',      'SENT',      DATE_SUB(NOW(), INTERVAL 26 MINUTE)),
('NLOG-005', 'ORD-1008', 'CUST-99', 'PUSH',  'Order ORD-1008 is in transit near Whitefield.',        'PENDING',   DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
('NLOG-006', 'ORD-1005', 'CUST-55', 'EMAIL', 'Dispatch delayed for ORD-1005 due to route allocation.','FAILED',    DATE_SUB(NOW(), INTERVAL 15 MINUTE))
ON DUPLICATE KEY UPDATE
message = VALUES(message),
status = VALUES(status),
sent_at = VALUES(sent_at);
