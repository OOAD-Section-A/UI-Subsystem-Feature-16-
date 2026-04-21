package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class TierDefinitionDAO extends BaseDAO<TierDefinition> {
    public TierDefinitionDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected TierDefinition mapRow(ResultSet rs) throws SQLException {
        TierDefinition t = new TierDefinition();
        t.setTierId(rs.getInt("tier_id"));
        t.setTierName(rs.getString("tier_name"));
        t.setMinSpendThreshold(rs.getDouble("min_spend_threshold"));
        t.setDefaultDiscountPct(rs.getDouble("default_discount_pct"));
        return t;
    }

    public List<TierDefinition> findAll() { return query("SELECT * FROM tier_definitions ORDER BY tier_id"); }

    public int insert(TierDefinition t) {
        return execute("INSERT INTO tier_definitions(tier_name,min_spend_threshold,default_discount_pct) VALUES(?,?,?)",
            t.getTierName(), t.getMinSpendThreshold(), t.getDefaultDiscountPct());
    }

    public int update(TierDefinition t) {
        return execute("UPDATE tier_definitions SET tier_name=?,min_spend_threshold=?,default_discount_pct=? WHERE tier_id=?",
            t.getTierName(), t.getMinSpendThreshold(), t.getDefaultDiscountPct(), t.getTierId());
    }

    public int delete(int id) { return execute("DELETE FROM tier_definitions WHERE tier_id=?", id); }
}
