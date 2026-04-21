package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class DiscountPolicyDAO extends BaseDAO<DiscountPolicy> {
    public DiscountPolicyDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected DiscountPolicy mapRow(ResultSet rs) throws SQLException {
        DiscountPolicy d = new DiscountPolicy();
        d.setPolicyId(rs.getString("policy_id"));
        d.setPolicyName(rs.getString("policy_name"));
        d.setStackingRule(rs.getString("stacking_rule"));
        d.setPriorityLevel(rs.getInt("priority_level"));
        d.setMaxDiscountCapPct(rs.getDouble("max_discount_cap_pct"));
        d.setPerishabilityDays(rs.getInt("perishability_days"));
        d.setClearanceDiscountPct(rs.getDouble("clearance_discount_pct"));
        return d;
    }

    public List<DiscountPolicy> findAll() { return query("SELECT * FROM discount_policies ORDER BY priority_level"); }

    public int insert(DiscountPolicy d) {
        return execute(
            "INSERT INTO discount_policies(policy_id,policy_name,stacking_rule,priority_level,max_discount_cap_pct,perishability_days,clearance_discount_pct) VALUES(?,?,?,?,?,?,?)",
            d.getPolicyId(), d.getPolicyName(), d.getStackingRule(), d.getPriorityLevel(),
            d.getMaxDiscountCapPct(), d.getPerishabilityDays(), d.getClearanceDiscountPct()
        );
    }

    public int update(DiscountPolicy d) {
        return execute(
            "UPDATE discount_policies SET policy_name=?,stacking_rule=?,priority_level=?,max_discount_cap_pct=?,perishability_days=?,clearance_discount_pct=? WHERE policy_id=?",
            d.getPolicyName(), d.getStackingRule(), d.getPriorityLevel(),
            d.getMaxDiscountCapPct(), d.getPerishabilityDays(), d.getClearanceDiscountPct(), d.getPolicyId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM discount_policies WHERE policy_id=?", id); }
}
