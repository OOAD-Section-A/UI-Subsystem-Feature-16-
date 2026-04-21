package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class OrderDAO extends BaseDAO<Order> {
    public OrderDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected Order mapRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getString("order_id"));
        o.setCustomerId(rs.getString("customer_id"));
        o.setOrderStatus(rs.getString("order_status"));
        o.setOrderDate(rs.getString("order_date"));
        o.setTotalAmount(rs.getDouble("total_amount"));
        o.setPaymentStatus(rs.getString("payment_status"));
        o.setSalesChannel(rs.getString("sales_channel"));
        return o;
    }

    public List<Order> findAll() { return query("SELECT * FROM orders ORDER BY order_date DESC LIMIT 500"); }
    public List<Order> findByStatus(String status) { return query("SELECT * FROM orders WHERE order_status=? ORDER BY order_date DESC", status); }
    public Order findById(String id) { return queryOne("SELECT * FROM orders WHERE order_id=?", id); }

    public int insert(Order o) {
        return execute(
            "INSERT INTO orders(order_id,customer_id,order_status,order_date,total_amount,payment_status,sales_channel) VALUES(?,?,?,NOW(),?,?,?)",
            o.getOrderId(), o.getCustomerId(), o.getOrderStatus(),
            o.getTotalAmount(), o.getPaymentStatus(), o.getSalesChannel()
        );
    }

    public int update(Order o) {
        return execute(
            "UPDATE orders SET order_status=?,total_amount=?,payment_status=?,sales_channel=? WHERE order_id=?",
            o.getOrderStatus(), o.getTotalAmount(), o.getPaymentStatus(), o.getSalesChannel(), o.getOrderId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM orders WHERE order_id=?", id); }
}
