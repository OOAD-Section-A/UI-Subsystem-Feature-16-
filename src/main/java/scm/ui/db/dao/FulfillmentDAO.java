package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class FulfillmentDAO extends BaseDAO<FulfillmentOrder> {
    public FulfillmentDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected FulfillmentOrder mapRow(ResultSet rs) throws SQLException {
        FulfillmentOrder f = new FulfillmentOrder();
        f.setFulfillmentId(rs.getString("fulfillment_id"));
        f.setOrderId(rs.getString("order_id"));
        f.setFulfillmentStatus(rs.getString("fulfillment_status"));
        f.setAssignedWarehouse(rs.getString("assigned_warehouse"));
        f.setPriorityLevel(rs.getString("priority_level"));
        f.setCreatedAt(rs.getString("created_at"));
        return f;
    }

    public List<FulfillmentOrder> findAll() { return query("SELECT * FROM fulfillment_orders ORDER BY created_at DESC LIMIT 200"); }
    public List<FulfillmentOrder> findByStatus(String status) { return query("SELECT * FROM fulfillment_orders WHERE fulfillment_status=?", status); }

    public int insert(FulfillmentOrder f) {
        return execute(
            "INSERT INTO fulfillment_orders(fulfillment_id,order_id,fulfillment_status,assigned_warehouse,priority_level,created_at) VALUES(?,?,?,?,?,NOW())",
            f.getFulfillmentId(), f.getOrderId(), f.getFulfillmentStatus(), f.getAssignedWarehouse(), f.getPriorityLevel()
        );
    }

    public int update(FulfillmentOrder f) {
        return execute(
            "UPDATE fulfillment_orders SET fulfillment_status=?,assigned_warehouse=?,priority_level=? WHERE fulfillment_id=?",
            f.getFulfillmentStatus(), f.getAssignedWarehouse(), f.getPriorityLevel(), f.getFulfillmentId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM fulfillment_orders WHERE fulfillment_id=?", id); }
}
