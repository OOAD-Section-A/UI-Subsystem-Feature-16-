package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class CustomerSegmentDAO extends BaseDAO<CustomerSegmentation> {
    public CustomerSegmentDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected CustomerSegmentation mapRow(ResultSet rs) throws SQLException {
        CustomerSegmentation c = new CustomerSegmentation();
        c.setSegmentationId(rs.getString("segmentation_id"));
        c.setCustomerId(rs.getString("customer_id"));
        c.setCumulativeSpend(rs.getDouble("cumulative_spend"));
        c.setAssignedTierId(rs.getInt("assigned_tier_id"));
        c.setManualOverride(rs.getBoolean("manual_override"));
        c.setOverrideTierId(rs.getInt("override_tier_id"));
        return c;
    }

    public List<CustomerSegmentation> findAll() { return query("SELECT * FROM customer_segmentation ORDER BY customer_id"); }

    public int insert(CustomerSegmentation c) {
        return execute(
            "INSERT INTO customer_segmentation(segmentation_id,customer_id,cumulative_spend,historical_order_totals,assigned_tier_id,manual_override) VALUES(?,?,?,?,?,?)",
            c.getSegmentationId(), c.getCustomerId(), c.getCumulativeSpend(), 0.00, c.getAssignedTierId(), c.isManualOverride()
        );
    }

    public int update(CustomerSegmentation c) {
        return execute(
            "UPDATE customer_segmentation SET cumulative_spend=?,assigned_tier_id=?,manual_override=?,override_tier_id=? WHERE segmentation_id=?",
            c.getCumulativeSpend(), c.getAssignedTierId(), c.isManualOverride(), c.getOverrideTierId(), c.getSegmentationId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM customer_segmentation WHERE segmentation_id=?", id); }
}
