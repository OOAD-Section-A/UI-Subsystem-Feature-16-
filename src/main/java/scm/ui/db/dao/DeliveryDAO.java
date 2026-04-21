package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class DeliveryDAO extends BaseDAO<DeliveryOrder> {
    public DeliveryDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected DeliveryOrder mapRow(ResultSet rs) throws SQLException {
        DeliveryOrder d = new DeliveryOrder();
        d.setDeliveryId(rs.getString("delivery_id"));
        d.setOrderId(rs.getString("order_id"));
        d.setCustomerId(rs.getString("customer_id"));
        d.setDeliveryAddress(rs.getString("delivery_address"));
        d.setDeliveryStatus(rs.getString("delivery_status"));
        d.setDeliveryType(rs.getString("delivery_type"));
        d.setDeliveryCost(rs.getDouble("delivery_cost"));
        d.setAssignedAgent(rs.getString("assigned_agent"));
        d.setWarehouseId(rs.getString("warehouse_id"));
        d.setCreatedAt(rs.getString("created_at"));
        return d;
    }

    public List<DeliveryOrder> findAll() { return query("SELECT * FROM delivery_orders ORDER BY created_at DESC LIMIT 200"); }
    public List<DeliveryOrder> findByStatus(String status) { return query("SELECT * FROM delivery_orders WHERE delivery_status=?", status); }

    public int insert(DeliveryOrder d) {
        return execute(
            "INSERT INTO delivery_orders(delivery_id,order_id,customer_id,delivery_address,delivery_status,delivery_type,delivery_cost,assigned_agent,warehouse_id,created_at) VALUES(?,?,?,?,?,?,?,?,?,NOW())",
            d.getDeliveryId(), d.getOrderId(), d.getCustomerId(), d.getDeliveryAddress(),
            d.getDeliveryStatus(), d.getDeliveryType(), d.getDeliveryCost(),
            d.getAssignedAgent(), d.getWarehouseId()
        );
    }

    public int update(DeliveryOrder d) {
        return execute(
            "UPDATE delivery_orders SET delivery_status=?,assigned_agent=?,delivery_cost=?,updated_at=NOW() WHERE delivery_id=?",
            d.getDeliveryStatus(), d.getAssignedAgent(), d.getDeliveryCost(), d.getDeliveryId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM delivery_orders WHERE delivery_id=?", id); }
}
