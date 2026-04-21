package scm.ui.db;

import scm.ui.db.dao.*;

/**
 * CREATIONAL PATTERN — FACTORY METHOD
 * DAOFactory produces the correct DAO instance for each subsystem.
 * Referenced in schema.sql pattern notes.
 */
public class DAOFactory {

    public enum DAOType {
        USER, SESSION, PANEL_STATE, NOTIFICATION, AUDIT_LOG,
        PRODUCT, STOCK_LEVEL, STOCK_MOVEMENT, STOCK_RECORD,
        ORDER, ORDER_ITEM, FULFILLMENT, DELIVERY, SHIPMENT,
        PRICE_LIST, PROMOTION, DISCOUNT_POLICY, CONTRACT_PRICING, PRICE_APPROVAL,
        COMMISSION, FORECAST, WAREHOUSE, BIN, EXCEPTION,
        TIER_DEFINITION, CUSTOMER_SEGMENT, BARCODE_EVENT
    }

    private DatabaseConnectionPool pool;

    public DAOFactory(DatabaseConnectionPool pool) {
        this.pool = pool;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseDAO<?>> T getDAO(DAOType type) {
        switch (type) {
            case USER:             return (T) new UserDAO(pool);
            case SESSION:          return (T) new SessionDAO(pool);
            case PANEL_STATE:      return (T) new PanelStateDAO(pool);
            case NOTIFICATION:     return (T) new NotificationDAO(pool);
            case AUDIT_LOG:        return (T) new AuditLogDAO(pool);
            case PRODUCT:          return (T) new ProductDAO(pool);
            case STOCK_LEVEL:      return (T) new StockLevelDAO(pool);
            case STOCK_MOVEMENT:   return (T) new StockMovementDAO(pool);
            case STOCK_RECORD:     return (T) new StockRecordDAO(pool);
            case ORDER:            return (T) new OrderDAO(pool);
            case ORDER_ITEM:       return (T) new OrderItemDAO(pool);
            case FULFILLMENT:      return (T) new FulfillmentDAO(pool);
            case DELIVERY:         return (T) new DeliveryDAO(pool);
            case SHIPMENT:         return (T) new ShipmentDAO(pool);
            case PRICE_LIST:       return (T) new PriceListDAO(pool);
            case PROMOTION:        return (T) new PromotionDAO(pool);
            case DISCOUNT_POLICY:  return (T) new DiscountPolicyDAO(pool);
            case CONTRACT_PRICING: return (T) new ContractPricingDAO(pool);
            case PRICE_APPROVAL:   return (T) new PriceApprovalDAO(pool);
            case COMMISSION:       return (T) new CommissionDAO(pool);
            case FORECAST:         return (T) new ForecastDAO(pool);
            case WAREHOUSE:        return (T) new WarehouseDAO(pool);
            case BIN:              return (T) new BinDAO(pool);
            case EXCEPTION:        return (T) new ExceptionDAO(pool);
            case TIER_DEFINITION:  return (T) new TierDefinitionDAO(pool);
            case CUSTOMER_SEGMENT: return (T) new CustomerSegmentDAO(pool);
            case BARCODE_EVENT:    return (T) new BarcodeEventDAO(pool);
            default:               throw new IllegalArgumentException("Unknown DAO type: " + type);
        }
    }
}
