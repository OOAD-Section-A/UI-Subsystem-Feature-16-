# SCM Nexus — UI Subsystem (Feature #16)

## Team Members

| Name | SRN |
|------|-----|
| TARUN S | PES1UG23AM919 |
| AKSHAY P SHETTI | PES1UG23AM039 |
| ADITYAA KUMAR H | PES1UG23AM025 |


A complete **Java Swing UI Subsystem** for the Supply Chain Management (SCM) application.
Implements full CRUD across **10 UI components** backed by a **MySQL OOAD database**, featuring
a dark luxury theme, live shipment tracking, demand forecasting, pricing workflows, real-time
delivery monitoring, and much more.

> **Integrated Subsystems:** This UI Subsystem (#16) integrates the following external subsystems:
> - **Inventory Subsystem** — in-memory stock operations via `InventoryUI` (addStock, removeStock, transferStock, getStock)
> - **Demand Forecasting Subsystem** — time-series query via `ForecastQueryService` JAR (`demand-forecasting-1.0-SNAPSHOT.jar`)
> - **Real-Time Delivery Monitoring Subsystem** — full lifecycle via `DeliveryMonitoringFacadeDB` JAR (`ramen-noodles-delivery-monitoring.jar`)
> - **Database Module** — `SupplyChainDatabaseFacade` and `InventoryAdapter` from `database-module-1.0.0-SNAPSHOT-standalone.jar`
> - **Exception Handler Subsystem** — `SCMExceptionHandler`, `SCMExceptionFactory`, `UISubsystem`, `InventorySubsystem` from `scm-exception-handler-v3.jar` (+ `jna-5.18.1.jar`, `jna-platform-5.18.1.jar` for Event Viewer integration)

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Integrated External Subsystems](#integrated-external-subsystems)
3. [10 UI Components & Features](#10-ui-components--features)
4. [Design Patterns](#design-patterns)
   - [Creational Patterns](#creational-patterns)
   - [Structural Patterns](#structural-patterns)
   - [Behavioural Patterns](#behavioural-patterns)
5. [SOLID Principles](#solid-principles)
6. [GRASP Principles](#grasp-principles)
7. [Database Schema Summary](#database-schema-summary)
8. [Prerequisites & Setup](#prerequisites--setup)
9. [Build & Run](#build--run)
10. [Demo Login Credentials](#demo-login-credentials)
11. [Project Structure](#project-structure)

---

## Project Overview

SCM Nexus is a full-featured supply chain management desktop application built with Java 17 and Swing.
It connects to a MySQL OOAD database (schema.sql + schema-extension.sql) and supports multiple user
roles with role-based access control. The UI is crafted with a custom dark "luxury" theme using
Java2D rendering — no external UI library required.

**Tech Stack:**
- Language: Java 17
- UI Framework: Java Swing (custom-themed, Java2D charts)
- Database: MySQL 8.0+ via JDBC (mysql-connector-j 8.3.0)
- Build Tool: Maven 3.8+ (fat JAR via maven-shade-plugin)
- Architecture: Layered (UI → Facade → DAO → DB)

---

## Integrated External Subsystems

### Inventory Subsystem (`src/main/inventory_subsystem/`)

Integrated directly as a Java source package. Exposes `InventoryUI` interface:

| Method | Signature | Purpose |
|--------|-----------|---------|
| `addStock` | `(sku, locationId, supplierId, quantity)` | Create batch, link supplier |
| `removeStock` | `(sku, locationId, supplierId, quantity)` | FIFO/FEFO deduction |
| `transferStock` | `(sku, fromLocation, toLocation, supplierId, quantity)` | Bin-to-bin transfer |
| `getStock` | `(sku, locationId, supplierId)` | Query current quantity |

Key classes: `InventoryService`, `InventoryRepository`, `InventoryExceptionSource`, `AddStockStrategy`,
`RemoveStockStrategy`, `TransferStockStrategy`, `InventoryBatch`, `InventoryItem`, `Supplier`, `IssuingPolicy`.

Exception codes mapped to Inventory subsystem methods: 110 (STOCK_UPDATE_CONFLICT),
166 (ITEM_NOT_FOUND), 167 (INSUFFICIENT_STOCK), 318 (DUPLICATE_SKU).
Codes outside the master register are routed as UNREGISTERED via v3 handler.

Wired in `SupplyChainFacade` via `inventoryAddStock`, `inventoryRemoveStock`, `inventoryTransferStock`,
`inventoryGetStock`. Handler registration in `SCMApplication` is no longer required (v3 self-routed flow).

---

### Demand Forecasting Subsystem (`lib/demand-forecasting-1.0-SNAPSHOT.jar`)

Uses `ForecastQueryService` to read from `forecast_timeseries` table in the OOAD DB.
Exposed in `ForecastPanel` → **Time-Series** and **JAR Integration** tabs. DB config is resolved in this order:
`-Ddb.*` → `DB_*` env vars → `database.properties` (`db.*`) → legacy `SCM_DB_*`.

Key classes used: `ForecastQueryService`, `ForecastSeriesResponseDto`, `ForecastPointDto`.

Falls back gracefully if the JAR or the `forecast_timeseries` table is unavailable, displaying an appropriate
status message in the UI without crashing.

---

### Real-Time Delivery Monitoring Subsystem (`lib/ramen-noodles-delivery-monitoring.jar`)

Full integration of Team Ramen Noodles (#9) system. Accessed through `SupplyChainFacade.getDeliverySystem()`.
Rendered in `DeliveryMonitoringPanel` (C-11) with 5 operational tabs.

Key classes used: `DeliveryMonitoringFacadeDB`, `DeliveryEventListener`, `DeliveryEventType`,
`Order`, `Rider`, `Customer`, `Device`, `GPSPing`, `PODRecord`, `ETARecord`, `Coordinate`.

All delivery events are bridged to the UI `EventBus` (`DELIVERY_STATUS_CHANGED`, `DELIVERY_GPS_UPDATED`,
`DELIVERY_POD_SUBMITTED`, `DELIVERY_ETA_UPDATED`). DB-mode fallback methods in `SupplyChainFacade` ensure
all operations still work when the JAR's internal flow cannot complete.

---

### Database Module (`lib/database-module-1.0.0-SNAPSHOT-standalone.jar`)

Used internally by `InventoryRepository` to persist stock changes back to `stock_levels` and related tables
via `SupplyChainDatabaseFacade` and `InventoryAdapter`.

---

### Exception Handler Subsystem (`lib/scm-exception-handler-v3.jar`)

`UIAuthExceptionSource` (singleton) routes authentication and settings exceptions to `UISubsystem.INSTANCE`
(exception IDs 252–258). `InventoryExceptionSource` routes inventory exceptions to
`InventorySubsystem.INSTANCE` (IDs 110, 166, 167, 318) and falls back to UNREGISTERED for others via
`SCMExceptionFactory.createUnregistered(...)` + `SCMExceptionHandler.INSTANCE.handle(...)`.

---

## 10 UI Components & Features

### C-01 — Authentication & RBAC (`auth/LoginPanel.java`)

- Secure login form with username and SHA-256 password hashing
- Role-Based Access Control (RBAC): 7 distinct roles — ADMIN, MANAGER, WAREHOUSE_STAFF, SALES_REP, CASHIER, ANALYST, DRIVER
- Account lockout after 5 consecutive failed login attempts (fires exception ID 256 via `UIAuthExceptionSource`)
- Session management via JWT tokens persisted in `ui_sessions` table
- Last-login timestamp tracking per user
- Splash screen on application startup with animated loading bar
- Demo mode fallback when database is unavailable

### C-02 — Navigation & Layout Controller (`navigation/NavigationPanel.java`)

- Sidebar navigation with role-filtered menu items (only shows panels the current role can access)
- Active panel highlighting with gold accent indicator bar
- Notification badge counter on the Notifications menu item, updated in real-time via `EventBus`
- Breadcrumb trail in the top header bar showing current panel
- User avatar chip with display name, email, and role label
- Persistent panel state — remembers the last active panel across sessions (stored in `ui_panel_state`)
- Sign Out button with confirmation dialog

### C-03 — Dashboard & Analytics (`dashboard/DashboardPanel.java`)

- **4 KPI Cards**: Total Orders, Total Revenue, Low Stock Alerts, Active Shipments — all loaded live from DB
- **Bar Chart** — Orders by Month/Day (Java2D, no external chart library)
- **Line Chart** — Revenue Trend over selected period (gradient fill under curve)
- **Pie/Donut Chart** — Order Status breakdown, interactive (click slice to see count + percentage)
- **Date Range Filter**: Last 7 Days, Last 30 Days, Last 90 Days, This Year
- **Recent Orders Summary Table** — paginated, up to 50 rows, with order details
- CSV Export of the summary table
- All data loaded asynchronously via `SwingWorker` to keep UI responsive
- Graceful date parsing supporting multiple timestamp formats

### C-04 — Inventory & Warehouse (`inventory/InventoryPanel.java`)

**7 sub-tabs:**

| Tab | Features |
|-----|----------|
| Products | Full CRUD — product name, SKU, category, sub-category, supplier, unit of measure, shelf life, storage conditions |
| Stock Levels | Current qty, reserved qty, available qty, reorder threshold, reorder quantity, safety stock; low-stock warning icon (⚠) |
| Movements | Record INBOUND, OUTBOUND, TRANSFER, ADJUSTMENT, RETURN movements with bin-to-bin tracking |
| Warehouses & Bins | Side-by-side CRUD panels for warehouses and bins; bin status management (AVAILABLE, OCCUPIED, RESERVED, DAMAGED) |
| Warehouse Product Stock | Read-only view of product quantities per bin per warehouse, aggregated from stock_records |
| Barcode / RFID | Simulate scanner events; `BarcodeReaderAdapter` converts raw scan strings into `BarcodeEvent` model objects |
| Stock Operations | Direct integration with **Inventory Subsystem** — Add, Remove, Transfer, Get stock with supplier ID; supplier management; FIFO/FEFO issuing policy; batch viewer; transaction log |

### C-05 — Order Management & Fulfillment (`orders/OrderPanel.java`)

**3 sub-tabs:**

| Tab | Features |
|-----|----------|
| Orders | Full CRUD — order status, total amount, payment status, sales channel; orders loaded from DB with status filtering |
| Order Items | Filter by Order ID; add/delete line items with unit price and auto-computed line total |
| Fulfillment | Create and manage fulfillment orders — warehouse assignment, priority level (HIGH/MEDIUM/LOW), status workflow |

### C-06 — Transport & Logistics (`logistics/LogisticsPanel.java`)

**3 sub-tabs:**

| Tab | Features |
|-----|----------|
| Deliveries | Full CRUD for delivery orders — status, type (STANDARD/EXPRESS/SAME_DAY/DROP_SHIP), cost, agent assignment |
| Shipments | Full CRUD — origin/destination address, package weight, priority, carrier, tracking ID, estimated cost |
| Live Map View | Simulated India map drawn with Java2D; plots active IN_TRANSIT and DISPATCHED shipments; route lines with dashed curves from origin → vehicle → destination; clickable markers show shipment details; live GPS coordinates if available, otherwise interpolated route position; city layer with major Indian cities; legend and detail card |

### C-07 — Pricing, Discount & Commission (`pricing/PricingPanel.java`)

**8 sub-tabs:**

| Tab | Features |
|-----|----------|
| Price List | Full CRUD — SKU, region, channel, price type, base price, price floor, currency, effective dates, status |
| Promotions | Full CRUD — promo name, coupon code, discount type (PERCENTAGE_OFF/FIXED_AMOUNT/BUY_X_GET_Y), value, dates, min cart value, max uses |
| Discount Policies | Full CRUD — stacking rules (EXCLUSIVE/ADDITIVE), priority level, max discount cap %, perishability days, clearance % |
| Contracts | B2B contract pricing — negotiated price per customer per SKU, validity dates, status |
| Approvals | Price approval workflow — request types, justification text, manager approval, status tracking (PENDING/APPROVED/REJECTED/ESCALATED) |
| Tiers | Customer tier definitions — tier name, minimum spend threshold, default discount % |
| Customer Segments | Customer-to-tier mapping with cumulative spend tracking and manual override support |
| Commissions | Agent commission ledger — period start/end, total sales, total commission amount |

### C-08 — Demand Forecasting & Reports (`forecast/ForecastPanel.java`)

**5 sub-tabs:**

| Tab | Features |
|-----|----------|
| Forecasts | Full CRUD — product ID, period, algorithm (MOVING_AVERAGE/EXPONENTIAL_SMOOTHING/ARIMA/ML_REGRESSION/SEASONAL_DECOMP), forecasted qty, confidence score, reorder signal |
| Time-Series | **Demand Forecasting Subsystem JAR integration** — `ForecastQueryService` fetches from `forecast_timeseries`; Java2D chart with forecast line, confidence upper/lower band; JTable view; CSV export |
| JAR Integration | Live integration probe page for updated forecasting JAR — validates query path for a product ID and reports connectivity/result status |
| Reorder Suggestions | Auto-aggregates low-stock items with matching forecast reorder signals; OUT OF STOCK / CRITICAL / LOW status indicators |
| Reports | Run-time report generator: Forecast Summary, Low Stock Report, Order Performance, Shipment Status; all exportable to CSV |

### C-09 — Notification & Exception Handling (`notifications/NotificationPanel.java`)

**3 sub-tabs:**

| Tab | Features |
|-----|----------|
| Notifications | View all notifications; mark individual as read (double-click) or mark all read; send new notification with type selection; badge count updates sidebar in real-time via `EventBus` |
| Exceptions | Log, view, and resolve system exceptions — subsystem code, severity (LOW/MEDIUM/HIGH/CRITICAL), message, status (OPEN/RESOLVED); real-time popup alert when exception is raised |
| Audit Log | Read-only audit trail viewer; configurable record limit; CSV export; immutable — no edit or delete allowed |

### C-10 — User & System Settings (`settings/SettingsPanel.java`)

**4 sub-tabs:**

| Tab | Features |
|-----|----------|
| User Management | Full CRUD for all users — username, display name, email, password, role, status; unlock locked accounts |
| My Profile | Edit own display name and email; change password with current-password verification and confirmation |
| Theme & UI | Accent colour swatches (Gold, Blue, Teal, Violet); font scale options |
| System Config | Key-value system configuration viewer and editor (mirrors `ui_system_config` table) |

### C-11 — Real-Time Delivery Monitoring (`delivery/DeliveryMonitoringPanel.java`)

**5 sub-tabs (Ramen Noodles #9 integration):**

| Tab | Features |
|-----|----------|
| Live GPS Tracking | Auto-refreshing table (every 30s) of active delivery orders with live GPS, ETA, pickup/dropoff addresses; real-time event log bridged from `DeliveryMonitoringFacadeDB` |
| Order Management | Create delivery, assign rider, update status, query order status/tracking URL |
| Fleet & Register | Register customer, rider, and GPS device; query rider position |
| POD & Completion | Complete delivery with ePOD (signature URL, photo URL, notes); query POD records |
| ETA & Controls | Query latest ETA; simulate GPS pings; view status history |

---

## Design Patterns

### Creational Patterns

| Pattern | Class / File | How It Is Used |
|---------|-------------|----------------|
| **Singleton** | `DatabaseConnectionPool` (`db/DatabaseConnectionPool.java`) | Guarantees exactly one JDBC connection pool exists for the entire application lifetime. Double-checked locking (`volatile` + `synchronized`) ensures thread safety during lazy initialisation. |
| **Singleton** | `SupplyChainFacade` (`patterns/SupplyChainFacade.java`) | A single facade instance acts as the sole service entry point for all UI panels. Prevents duplicate DAO factories from being created. |
| **Singleton** | `EventBus` (`patterns/EventBus.java`) | One event bus instance shared across all subsystems so panels can publish and subscribe without holding direct references to each other. |
| **Singleton** | `UIAuthExceptionSource` (`exceptions/UIAuthExceptionSource.java`) | Single instance routes all UI authentication exceptions to the central `UISubsystem.INSTANCE` handler, preventing multiple handler registrations. |
| **Factory Method** | `DAOFactory` (`db/DAOFactory.java`) | `getDAO(DAOType)` produces the correct concrete DAO (`OrderDAO`, `ShipmentDAO`, `ForecastDAO`, etc.) for a given enum type without the caller knowing the concrete class. Covers 26 DAO types. |
| **Builder** | `CrudPanel.FormBuilder` (`util/CrudPanel.java`) | Fluent builder for dialog forms — chains `addField()` and `addSeparator()` calls and calls `build()` to produce a ready-to-display `JPanel`. Used in every add/edit dialog across all 10 components. |

### Structural Patterns

| Pattern | Class / File | How It Is Used |
|---------|-------------|----------------|
| **Facade** | `SupplyChainFacade` (`patterns/SupplyChainFacade.java`) | Single entry point for all ~80+ service operations (products, stock, orders, shipments, pricing, forecasts, exceptions, notifications, audit, users, delivery monitoring). UI panels never touch DAOs directly — they only call facade methods. Also acts as the bridge between the UI layer and all integrated external subsystems (Inventory, Delivery, Forecasting). |
| **Adapter** | `BarcodeReaderAdapter` (`patterns/BarcodeReaderAdapter.java`) | Adapts a raw scan string (from any scanner device or a `RawBarcodeSource` interface) into a fully populated `BarcodeEvent` model object. Decouples scanner hardware protocol from application logic. Contains `SimulatedScanner` for demo/testing. |
| **Template Method** | `BaseDAO<T>` (`db/dao/BaseDAO.java`) | Defines the CRUD scaffolding (`query()`, `execute()`, `queryOne()`, `executeAndGetKey()`). All 26 concrete DAOs extend `BaseDAO` and implement only `mapRow(ResultSet)`, which maps a result row to a domain object. |
| **Template Method** | `CrudPanel` (`util/CrudPanel.java`) | Defines the full CRUD panel lifecycle: toolbar, table, search, async loading, dialog wiring. Subclasses implement `loadData()`, `openAddDialog()`, `openEditDialog()`, and `deleteRow()`. Used by every sub-panel across C-04 through C-10. |
| **Template Method** | `AddStockStrategy`, `RemoveStockStrategy`, `TransferStockStrategy` (`inventory_subsystem/`) | All implement `StockOperationStrategy` interface defining `execute()`. `RemoveStockStrategy` also performs FIFO/FEFO sort before deduction. `TransferStockStrategy` has an additional `addToDestination()` method. |
| **Composite** | `NavigationPanel` menu items (`navigation/NavigationPanel.java`) | Menu items are defined as an array of `MenuItem` descriptors forming a tree structure. The sidebar iterates the composite to render only nodes the current user role has access to. |

### Behavioural Patterns

| Pattern | Class / File | How It Is Used |
|---------|-------------|----------------|
| **Observer** | `EventBus` (`patterns/EventBus.java`) | Publish-subscribe event bus with 26 event types (USER_LOGGED_IN, STOCK_CHANGED, ORDER_CREATED, SHIPMENT_UPDATED, NOTIFICATION_RECEIVED, EXCEPTION_RAISED, DELIVERY_STATUS_CHANGED, DELIVERY_GPS_UPDATED, DELIVERY_POD_SUBMITTED, DELIVERY_ETA_UPDATED, etc.). Panels subscribe to events they care about; controllers publish events after every state change. `ConcurrentHashMap` ensures thread safety. |
| **Observer** | `DeliveryMonitoringFacadeDB` event bridge (`patterns/SupplyChainFacade.java`) | `wireDeliveryEventsToEventBus()` subscribes to all `DeliveryEventType` events from the delivery JAR and republishes them to the UI `EventBus`, decoupling the delivery subsystem from all UI panels. |
| **Strategy** | `AppUtils.CsvExportStrategy` and `AppUtils.TxtExportStrategy` (`util/AppUtils.java`) | Both implement the `ExportStrategy` interface. Callers pass a `JTable` and the strategy handles file-picker dialog, data extraction, and file writing. Swap strategies without changing the caller. |
| **Strategy** | `AddStockStrategy`, `RemoveStockStrategy`, `TransferStockStrategy` (`inventory_subsystem/`) | All implement `StockOperationStrategy`. `InventoryService` holds references to all three and delegates based on the operation type, allowing independent variation of add, remove, and transfer algorithms. |

---

## SOLID Principles

| Principle | Where Applied | File(s) | Explanation |
|-----------|--------------|---------|-------------|
| **Single Responsibility (SRP)** | All 26 DAO classes (`OrderDAO`, `ShipmentDAO`, `ForecastDAO`, etc.) | `db/dao/*.java` | Each DAO owns exactly one entity's persistence logic — no DAO mixes concerns with another table. |
| **SRP** | `DatabaseConnectionPool` | `db/DatabaseConnectionPool.java` | Responsible only for managing JDBC connections; nothing else. |
| **SRP** | `LuxuryTheme` | `util/LuxuryTheme.java` | Responsible only for visual styling — colors, fonts, and component factories. No business logic. |
| **SRP** | `AppUtils` | `util/AppUtils.java` | Owns utility concerns: ID generation, password hashing, date formatting, dialog helpers, and export strategies. No UI layout or DB access. |
| **SRP** | `InventoryExceptionSource` | `inventory_subsystem/InventoryExceptionSource.java` | Solely responsible for routing inventory exception IDs to `InventorySubsystem.INSTANCE` (or UNREGISTERED fallback). No stock logic. |
| **SRP** | `UIAuthExceptionSource` | `exceptions/UIAuthExceptionSource.java` | Solely responsible for routing UI/auth exception IDs (252–258) to `UISubsystem.INSTANCE`. |
| **SRP** | `AddStockStrategy`, `RemoveStockStrategy`, `TransferStockStrategy` | `inventory_subsystem/` | Each strategy class is responsible for exactly one stock operation algorithm. |
| **Open/Closed (OCP)** | `price_list`, `tier_definitions`, `discount_policies` tables | `schema.sql` | New price types, tier levels, or discount rules are added as new rows — no table structure changes required. The Discount Rules Engine reads policies dynamically. |
| **OCP** | `DAOFactory` with `DAOType` enum | `db/DAOFactory.java` | Adding a new DAO requires adding one enum value and one `case` — existing cases are never modified. |
| **OCP** | `ExportStrategy` interface | `util/AppUtils.java` | New export formats (PDF, Excel, etc.) can be added as new strategy implementations without touching existing export callers. |
| **OCP** | `EventBus.Event` enum | `patterns/EventBus.java` | New cross-subsystem events (including the 4 delivery events) are added as new enum values without changing subscriber code. |
| **OCP** | `StockOperationStrategy` interface | `inventory_subsystem/StockOperationStrategy.java` | New stock operations (e.g., QUARANTINE, WRITE_OFF) can be added as new strategy classes without modifying `InventoryService`. |
| **Liskov Substitution (LSP)** | All 26 DAOs extending `BaseDAO<T>` | `db/dao/*.java` | Every concrete DAO can be used anywhere a `BaseDAO` is expected. The `mapRow()` contract is honoured by all subclasses. |
| **LSP** | All `CrudPanel` subclasses | `inventory/`, `orders/`, `logistics/`, etc. | Every sub-panel (e.g., `OrderCrudPanel`, `ForecastCrudPanel`) correctly substitutes for `CrudPanel` wherever the template expects `loadData()`, `openAddDialog()`, etc. |
| **LSP** | `AddStockStrategy`, `RemoveStockStrategy`, `TransferStockStrategy` | `inventory_subsystem/` | All correctly substitute for `StockOperationStrategy`; each `execute()` implementation honours the contract of validating input, delegating to repository, and firing exceptions on error. |
| **Interface Segregation (ISP)** | `ExportStrategy` interface | `util/AppUtils.java` | The export interface exposes only one method (`export(JTable, Component)`). Implementors are not forced to implement unrelated methods. |
| **ISP** | `NotificationPanel.BadgeCallback` | `notifications/NotificationPanel.java` | A narrow single-method interface so `MainFrame` can wire badge updates without coupling to the full `NotificationPanel`. |
| **ISP** | `BarcodeReaderAdapter.RawBarcodeSource` | `patterns/BarcodeReaderAdapter.java` | A minimal interface (3 methods) that any scanner device class can implement, without inheriting unrelated scanner behaviour. |
| **ISP** | `NavigationPanel.PanelSwitchListener` | `navigation/NavigationPanel.java` | Single-method interface for panel navigation callbacks — `MainFrame` implements only what it needs. |
| **ISP** | `InventoryUI` | `inventory_subsystem/InventoryUI.java` | Exposes only the 4 stock operation methods the UI needs (addStock, removeStock, transferStock, getStock). Supplier management and issuing policy are separate concerns on `InventoryService`. |
| **ISP** | `InventoryDataStore` | `inventory_subsystem/InventoryDataStore.java` | Minimal interface exposing only `find`, `save`, and `exists` — `InventoryRepository` can add transaction/supplier tracking without the interface growing. |
| **Dependency Inversion (DIP)** | `SupplyChainFacade` consuming DAOs via `DAOFactory` | `patterns/SupplyChainFacade.java`, `db/DAOFactory.java` | The facade depends on the `DAOFactory` abstraction, not on concrete DAO classes directly. Swapping a DAO implementation requires only a one-line change in `DAOFactory`. |
| **DIP** | UI panels depending only on `SupplyChainFacade` | All panel files | All panels (`DashboardPanel`, `OrderPanel`, etc.) depend on the facade abstraction rather than on concrete DAO or DB classes. The DB layer is fully hidden. |
| **DIP** | `BaseDAO<T>` depending on `DatabaseConnectionPool` | `db/dao/BaseDAO.java` | DAOs receive the pool via constructor injection, not by calling a static factory — making them testable with alternative pool implementations. |
| **DIP** | `InventoryService` consuming `InventoryDataStore` | `inventory_subsystem/InventoryService.java` | `InventoryService` depends on the `InventoryDataStore` abstraction; the concrete `InventoryRepository` is substitutable. |

---

## GRASP Principles

| Principle | Where Applied | File(s) | Explanation |
|-----------|--------------|---------|-------------|
| **Information Expert** | Each DAO class owns its own SQL | `db/dao/*.java` | `OrderDAO` knows how to query, insert, update, and delete orders because it is the information expert for the `orders` table. No other class holds order SQL. |
| **Information Expert** | `stock_records` owns current quantity | `db/dao/StockRecordDAO.java` | The table that stores stock is the natural expert for "how much stock exists in a bin" — the `StockRecordDAO` exposes this via `sumByProduct`, `sumByProductAndBin`, `sumByProductAndWarehouse`. |
| **Information Expert** | `vw_inventory_stock_report` SQL view | `schema.sql` | Reporting queries live in the DB view where the data resides, not scattered across Java classes. |
| **Information Expert** | `InventoryItem` owns batch quantities | `inventory_subsystem/InventoryItem.java` | `getTotalQuantity()` sums across all batches — the item is the expert about its own total stock. |
| **Creator** | `DAOFactory` creates all DAOs | `db/DAOFactory.java` | The factory has the information needed to instantiate DAOs (the DB pool), so it is the natural creator. No panel creates a DAO directly. |
| **Creator** | `SupplyChainFacade` creates domain events | `patterns/SupplyChainFacade.java` | The facade knows when a business action occurs (order created, stock changed, delivery event received) and is therefore responsible for publishing the corresponding `EventBus` event. |
| **Creator** | `InventoryService` creates `InventoryBatch` on `addStock` | `inventory_subsystem/AddStockStrategy.java` | `AddStockStrategy` creates new `InventoryBatch` objects because it has all the context needed (quantity, arrival time, supplier). |
| **Controller** | `SupplyChainFacade` | `patterns/SupplyChainFacade.java` | Acts as the system controller — it receives all UI requests, delegates to the correct DAO, fires events, and writes audit logs. UI panels do not contain business logic. |
| **Low Coupling** | Cross-subsystem VARCHAR FKs in schema | `schema.sql` | `order_id`, `customer_id`, `product_id` are referenced as plain `VARCHAR` across subsystem boundaries (not enforced DB FKs), so external teams can evolve their PKs without breaking this schema. |
| **Low Coupling** | `EventBus` pub/sub | `patterns/EventBus.java` | Panels that need to react to each other's changes (e.g., notification badge after an order event, live tracking refresh after delivery GPS update) do so through the event bus rather than direct method calls — eliminating compile-time dependencies between panels. |
| **Low Coupling** | `SupplyChainFacade.getDeliverySystem()` wrapper | `patterns/SupplyChainFacade.java` | `DeliveryMonitoringPanel` never imports `DeliveryMonitoringFacadeDB` directly for core operations — it always calls facade wrapper methods, so swapping the delivery JAR only touches the facade. |
| **High Cohesion** | Each sub-panel class | `inventory/InventoryPanel.java`, `forecast/ForecastPanel.java`, etc. | `ForecastCrudPanel`, `ReorderSuggestionsPanel`, `TimeSeriesPanel`, `TrendChartPanel` each have one focused responsibility. The parent `ForecastPanel` only composes them into tabs. |
| **High Cohesion** | `LuxuryTheme` | `util/LuxuryTheme.java` | All styling code is in one cohesive class — color constants, font constants, and component factory methods — nothing else. |
| **High Cohesion** | `InventoryService` | `inventory_subsystem/InventoryService.java` | Cohesively manages the four core inventory operations plus supplier and issuing-policy management. All concerns relate directly to in-memory inventory state. |
| **Polymorphism** | `BaseDAO<T>.mapRow()` | `db/dao/BaseDAO.java` | The template method is abstract; each concrete DAO provides its own polymorphic `mapRow` implementation. The query infrastructure works for all 26 entity types without a single `instanceof` check. |
| **Polymorphism** | `CrudPanel` abstract methods | `util/CrudPanel.java` | `loadData()`, `openAddDialog()`, `openEditDialog()`, `deleteRow()` are polymorphic — the toolbar buttons call them without knowing which concrete sub-panel is active. |
| **Polymorphism** | `StockOperationStrategy.execute()` | `inventory_subsystem/StockOperationStrategy.java` | `InventoryService` calls `execute()` on whichever strategy is relevant; the caller never uses `instanceof`. |
| **Pure Fabrication** | `AppUtils` | `util/AppUtils.java` | Not a domain concept, but a cohesive utility class created purely to achieve high cohesion and low coupling. Keeps ID generation, password hashing, and export logic out of UI and DAO classes. |
| **Pure Fabrication** | `LuxuryTheme` | `util/LuxuryTheme.java` | Has no domain meaning — it exists purely as a design fabrication to centralise all visual styling and prevent duplication. |
| **Pure Fabrication** | `EventBus` | `patterns/EventBus.java` | Not a real-world concept — fabricated to achieve low coupling between panels that need to communicate. |
| **Indirection** | `SupplyChainFacade` between UI and DAOs | `patterns/SupplyChainFacade.java` | Introduces a facade as an indirection layer so UI panels never couple directly to DAOs or the connection pool. This protects against DAO refactoring cascading into UI code. |
| **Indirection** | `EventBus` between publishers and subscribers | `patterns/EventBus.java` | Events flow through the bus as an intermediate object, so publishers (facade) and subscribers (panels) never reference each other directly. |
| **Indirection** | `BarcodeReaderAdapter` between scanner devices and app | `patterns/BarcodeReaderAdapter.java` | Adapts raw device output through an intermediate `RawBarcodeSource` interface, so scanner hardware changes never propagate into application logic. |
| **Protected Variations** | `DatabaseConnectionPool` env-var overrides | `db/DatabaseConnectionPool.java` | Connection URL, user, and password can be overridden at runtime via `SCM_DB_URL`, `SCM_DB_USER`, `SCM_DB_PASSWORD` — the rest of the application is protected from this variation. |
| **Protected Variations** | DEMO mode in `DatabaseConnectionPool` | `db/DatabaseConnectionPool.java` | If the DB is unreachable, the pool initialises with zero connections and logs a warning. The application launches in demo mode — all other code is protected from the variation of DB availability. |
| **Protected Variations** | Delivery JAR null-check + DB fallback in `SupplyChainFacade` | `patterns/SupplyChainFacade.java` | Every delivery operation checks `if (deliverySystem == null)` and falls back to direct SQL on the OOAD DB, protecting the entire UI from the variation of the JAR being absent. |
| **Protected Variations** | `ForecastPanel` graceful fallback | `forecast/ForecastPanel.java` | `TimeSeriesPanel` wraps `ForecastQueryService` in a `SwingWorker` with try/catch; if the JAR or DB is unavailable, only the status label updates — the rest of the panel remains functional. |

---

## Database Schema Summary

The OOAD database contains tables across these subsystems:

| Subsystem | Key Tables |
|-----------|-----------|
| Multi-level Pricing | `price_list`, `tier_definitions`, `customer_segmentation`, `price_configuration`, `discount_rule_results`, `promotions`, `discount_policies`, `contract_pricing`, `price_approvals` |
| Warehouse Management | `warehouses`, `warehouse_zones`, `bins`, `goods_receipts`, `stock_records`, `stock_movements`, `pick_tasks`, `staging_dispatch`, `warehouse_returns`, `cycle_counts` |
| Reporting (read-only views) | `vw_inventory_stock_report`, `vw_price_discount_report`, `vw_exception_report` |
| UI Subsystem | `ui_users`, `ui_sessions`, `ui_panel_state`, `ui_notifications`, `ui_audit_log`, `ui_notification_preferences`, `ui_system_config` |
| Double-Entry Stock Keeping | `stock_ledger_entries` |
| Inventory Management | `products`, `product_batches`, `expiry_tracking`, `stock_levels`, `stock_adjustments`, `reorder_management`, `stock_reservations`, `stock_freeze`, `dead_stock`, `stock_valuation` |
| Orders | `orders`, `order_items`, `fulfillment_orders`, `packing_details` |
| Delivery & Tracking | `delivery_orders`, `delivery_tracking_routes`, `delivery_tracking_waypoints`, `delivery_tracking_events` |
| Transport & Logistics | `shipments`, `logistics_routes`, `shipment_alerts` |
| Packaging & Returns | `packaging_jobs`, `repair_requests`, `receipt_records`, `product_returns`, `return_growth_statistics` |
| Demand Forecasting | `demand_forecasts`, `sales_records`, `holiday_calendar`, `promotional_calendar`, `product_lifecycle_stages`, `forecast_performance_metrics` |
| Commission Tracking | `agents`, `commission_tiers`, `commission_sales`, `commission_history` |
| Notifications & Exceptions | `ui_notifications`, `subsystem_exceptions`, `barcode_rfid_events` |
| Delivery Monitoring (DB Mode) | `customers`, `riders`, `devices`, `gps_pings`, `delivery_status_log`, `pod_records`, `notification_logs` |
| Inventory Metadata (auto-created) | `inventory_suppliers`, `inventory_settings` |

---

## Prerequisites & Setup

- **Java 17+**
- **Maven 3.8+**
- **MySQL 8.0+** with the `OOAD` database

### Required JARs (place in `SCM_UI/lib/`)

| JAR | Purpose |
|-----|---------|
| `demand-forecasting-1.0-SNAPSHOT.jar` | Demand Forecasting Subsystem — `ForecastQueryService` |
| `ramen-noodles-delivery-monitoring.jar` | Real-Time Delivery Monitoring — `DeliveryMonitoringFacadeDB` |
| `database-module-1.0.0-SNAPSHOT-standalone.jar` | DB Module used by Inventory Subsystem |
| `scm-exception-handler-v3.jar` | Exception handler v3 — `UISubsystem.INSTANCE`, `SCMExceptionFactory` |
| `scm-exception-viewer-gui.jar` | Exception Viewer GUI launcher |
| `jna-5.18.1.jar` | JNA runtime required by Event Viewer integration |
| `jna-platform-5.18.1.jar` | JNA platform bindings required by Event Viewer integration |

### Database Setup

```sql
SOURCE schema.sql;
SOURCE schema-extension.sql;
SOURCE sample-data.sql;   -- optional: loads realistic dummy data
SOURCE sample-data-delivery-db.sql;  -- optional: loads delivery monitoring seed data
```

### Connection Configuration

The app and the shared database module resolve config in this order:

1. JVM system properties (`-Ddb.url`, `-Ddb.username`, `-Ddb.password`, `-Ddb.pool.size`)
2. Environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_SIZE`)
3. `database.properties` (`db.url`, `db.username`, `db.password`, `db.pool.size`)
4. Legacy env fallbacks (`SCM_DB_URL`, `SCM_DB_USER`, `SCM_DB_PASSWORD`)

Default fallback values in `DatabaseConnectionPool.java`:

```
URL:      jdbc:mysql://localhost:3306/OOAD?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
User:     root
Password: tarun12345
```

Override at runtime using environment variables:

```bash
export DB_URL="jdbc:mysql://localhost:3306/OOAD?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="your_user"
export DB_PASSWORD="your_password"
export DB_POOL_SIZE="10"
```

---

## Build & Run

```bash
# 1. Place required JARs in SCM_UI/lib/
#    demand-forecasting-1.0-SNAPSHOT.jar
#    ramen-noodles-delivery-monitoring.jar
#    database-module-1.0.0-SNAPSHOT-standalone.jar
#    scm-exception-handler-v3.jar
#    scm-exception-viewer-gui.jar
#    jna-5.18.1.jar
#    jna-platform-5.18.1.jar

# 2. Build the fat JAR (includes MySQL connector + all JARs)
cd SCM_UI
mvn clean package -q

# 3. Run the application
java -jar target/scm-nexus-ui-1.0.0.jar
```

**Without Maven:**

```bash
# Download mysql-connector-j-8.3.0.jar and place in lib/
mkdir -p out
javac -cp "lib/mysql-connector-j-8.3.0.jar:lib/demand-forecasting-1.0-SNAPSHOT.jar:lib/ramen-noodles-delivery-monitoring.jar:lib/database-module-1.0.0-SNAPSHOT-standalone.jar:lib/scm-exception-handler-v3.jar:lib/scm-exception-viewer-gui.jar:lib/jna-5.18.1.jar:lib/jna-platform-5.18.1.jar" \
      -sourcepath src/main/java:src/main/inventory_subsystem \
      -d out \
      src/main/java/scm/ui/SCMApplication.java

java -cp "out:lib/mysql-connector-j-8.3.0.jar:lib/demand-forecasting-1.0-SNAPSHOT.jar:lib/ramen-noodles-delivery-monitoring.jar:lib/database-module-1.0.0-SNAPSHOT-standalone.jar:lib/scm-exception-handler-v3.jar:lib/scm-exception-viewer-gui.jar:lib/jna-5.18.1.jar:lib/jna-platform-5.18.1.jar" scm.ui.SCMApplication
```

> **Note (Windows):** Replace `:` with `;` in classpath arguments on Windows.

---

## Demo Login Credentials

Seeded automatically on first launch if the user table is empty:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ADMIN |
| `manager` | `manager123` | MANAGER |
| `warehouse` | `staff123` | WAREHOUSE_STAFF |
| `sales` | `sales123` | SALES_REP |
| `analyst` | `analyst123` | ANALYST |
| `driver` | `driver123` | DRIVER |

> **DEMO Mode:** If MySQL is unavailable, the app runs with empty tables and no persistence. All UI panels remain accessible.

---

## Project Structure

```
SCM_UI/
├── pom.xml
├── schema.sql                              ← Main database schema (13 subsystems)
├── schema-extension.sql                    ← Extension tables (tracking, packaging, returns, delivery monitoring)
├── sample-data.sql                         ← Comprehensive dummy data for all tables
├── sample-data-delivery-db.sql             ← Delivery monitoring seed data (customers, riders, GPS pings)
├── database.properties                     ← DB connection reference
└── src/main/
    ├── inventory_subsystem/                ← Inventory Subsystem (integrated source package)
    │   ├── InventoryUI.java                ← Interface: addStock, removeStock, transferStock, getStock
    │   ├── InventoryService.java           ← Implements InventoryUI; owns strategies + policy
    │   ├── InventoryRepository.java        ← Implements InventoryDataStore; persists to DB via adapter
    │   ├── InventoryDataStore.java         ← Interface: find, save, exists
    │   ├── InventoryItem.java              ← Domain entity; holds List<InventoryBatch>
    │   ├── InventoryBatch.java             ← Batch with quantity, arrival/expiry, serial numbers
    │   ├── InventoryExceptionSource.java   ← Routes to InventorySubsystem (v3) and UNREGISTERED fallback
    │   ├── AddStockStrategy.java           ← Strategy: validate → create/update batch → save → check thresholds
    │   ├── RemoveStockStrategy.java        ← Strategy: FIFO/FEFO sort → deduct → save → check thresholds
    │   ├── TransferStockStrategy.java      ← Strategy: remove from source + add to destination
    │   ├── StockOperationStrategy.java     ← Interface: execute(sku, location, supplier, qty, repo, exSrc, policy)
    │   ├── IssuingPolicy.java              ← Enum: FIFO, FEFO
    │   ├── StockTransaction.java           ← Value object: sku, location, supplier, delta, type, timestamp
    │   └── Supplier.java                   ← Value object: supplierId, name, leadTimeDays, performanceRating
    └── java/scm/ui/
        ├── SCMApplication.java             ← main() — splash, DB init, user seeding, subsystem startup
        ├── MainFrame.java                  ← JFrame shell — login/app card layout, EventBus subscriptions
        ├── db/
        │   ├── DatabaseConnectionPool.java ← Singleton — JDBC pool with env-var overrides
        │   ├── DAOFactory.java             ← Factory Method — produces 26 DAO types
        │   └── dao/
        │       ├── BaseDAO.java            ← Template Method — CRUD scaffolding
        │       ├── OrderDAO.java           ← Orders persistence
        │       ├── ShipmentDAO.java        ← Shipments + live map points query
        │       ├── ForecastDAO.java        ← Demand forecasts persistence
        │       ├── PriceListDAO.java       ← Price list persistence
        │       ├── UserDAO.java            ← User accounts, login, locking
        │       ├── StockRecordDAO.java     ← Stock records; sumByProduct/Bin/Warehouse aggregates
        │       ├── BinDAO.java             ← Bins + ensureDefaultBinForWarehouse
        │       └── ... (19 more DAOs)
        ├── model/
        │   └── ... (27 entity classes + WarehouseProductStock, ShipmentMapPoint)
        ├── patterns/
        │   ├── EventBus.java               ← Observer — 26 event types including 4 delivery events
        │   ├── SupplyChainFacade.java       ← Facade — ~100+ service operations + subsystem bridges
        │   └── BarcodeReaderAdapter.java   ← Adapter — raw scan → BarcodeEvent; SimulatedScanner
        ├── util/
        │   ├── LuxuryTheme.java            ← Design system — colors, fonts, components
        │   ├── AppUtils.java               ← Strategy (CSV/TXT export), utilities
        │   └── CrudPanel.java              ← Template Method + Builder — reusable CRUD panel
        ├── exceptions/
        │   └── UIAuthExceptionSource.java  ← Singleton bridge to UISubsystem.INSTANCE (IDs 252–258)
        ├── auth/LoginPanel.java            ← C-01 — Authentication & RBAC
        ├── navigation/NavigationPanel.java ← C-02 — Navigation & Layout Controller
        ├── dashboard/DashboardPanel.java   ← C-03 — Dashboard & Analytics
        ├── inventory/InventoryPanel.java   ← C-04 — Inventory & Warehouse (7 tabs including Stock Operations)
        ├── orders/OrderPanel.java          ← C-05 — Order Management & Fulfillment
        ├── logistics/LogisticsPanel.java   ← C-06 — Transport & Logistics + Live Map
        ├── pricing/PricingPanel.java       ← C-07 — Pricing, Discount & Commission (8 tabs)
        ├── forecast/ForecastPanel.java     ← C-08 — Demand Forecasting & Reports (ForecastQueryService integration)
        ├── notifications/NotificationPanel.java ← C-09 — Notifications & Exceptions
        ├── settings/SettingsPanel.java     ← C-10 — User & System Settings
        └── delivery/DeliveryMonitoringPanel.java ← C-11 — Real-Time Delivery Monitoring (5 tabs)
```
