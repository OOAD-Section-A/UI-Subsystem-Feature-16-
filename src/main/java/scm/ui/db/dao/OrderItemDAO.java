package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class OrderItemDAO extends BaseDAO<OrderItem> {
    public OrderItemDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected OrderItem mapRow(ResultSet rs) throws SQLException {
        OrderItem i = new OrderItem();
        i.setOrderItemId(rs.getString("order_item_id"));
        i.setOrderId(rs.getString("order_id"));
        i.setProductId(rs.getString("product_id"));
        i.setOrderedQuantity(rs.getInt("ordered_quantity"));
        i.setUnitPrice(rs.getDouble("unit_price"));
        i.setLineTotal(rs.getDouble("line_total"));
        return i;
    }

    public List<OrderItem> findAll() {
        return query("SELECT * FROM order_items ORDER BY order_id, order_item_id");
    }

    public List<OrderItem> findByOrder(String orderId) { return query("SELECT * FROM order_items WHERE order_id=?", orderId); }

    public int insert(OrderItem i) {
        return execute(
            "INSERT INTO order_items(order_item_id,order_id,product_id,ordered_quantity,unit_price,line_total) VALUES(?,?,?,?,?,?)",
            i.getOrderItemId(), i.getOrderId(), i.getProductId(), i.getOrderedQuantity(), i.getUnitPrice(), i.getLineTotal()
        );
    }

    public int update(OrderItem i) {
        return execute(
            "UPDATE order_items SET ordered_quantity=?,unit_price=?,line_total=? WHERE order_item_id=?",
            i.getOrderedQuantity(), i.getUnitPrice(), i.getLineTotal(), i.getOrderItemId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM order_items WHERE order_item_id=?", id); }
}
