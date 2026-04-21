package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class CommissionDAO extends BaseDAO<CommissionEntry> {
    public CommissionDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected CommissionEntry mapRow(ResultSet rs) throws SQLException {
        CommissionEntry c = new CommissionEntry();
        c.setCommissionId(rs.getString("commission_id"));
        c.setAgentId(rs.getString("agent_id"));
        c.setPeriodStart(rs.getString("period_start"));
        c.setPeriodEnd(rs.getString("period_end"));
        c.setTotalSales(rs.getDouble("total_sales"));
        c.setTotalCommission(rs.getDouble("total_commission"));
        c.setCalculatedAt(rs.getString("calculated_at"));
        return c;
    }

    public List<CommissionEntry> findAll() { return query("SELECT * FROM commission_history ORDER BY calculated_at DESC"); }
    public List<CommissionEntry> findByAgent(String agentId) { return query("SELECT * FROM commission_history WHERE agent_id=?", agentId); }

    public int insert(CommissionEntry c) {
        return execute(
            "INSERT INTO commission_history(commission_id,agent_id,period_start,period_end,total_sales,total_commission,calculated_at) VALUES(?,?,?,?,?,?,NOW())",
            c.getCommissionId(), c.getAgentId(), c.getPeriodStart(), c.getPeriodEnd(),
            c.getTotalSales(), c.getTotalCommission()
        );
    }

    public int update(CommissionEntry c) {
        return execute(
            "UPDATE commission_history SET total_sales=?,total_commission=? WHERE commission_id=?",
            c.getTotalSales(), c.getTotalCommission(), c.getCommissionId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM commission_history WHERE commission_id=?", id); }
}
