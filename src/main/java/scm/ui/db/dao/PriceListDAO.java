package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class PriceListDAO extends BaseDAO<PriceListEntry> {
    public PriceListDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected PriceListEntry mapRow(ResultSet rs) throws SQLException {
        PriceListEntry e = new PriceListEntry();
        e.setPriceId(rs.getString("price_id"));
        e.setSkuId(rs.getString("sku_id"));
        e.setRegionCode(rs.getString("region_code"));
        e.setChannel(rs.getString("channel"));
        e.setPriceType(rs.getString("price_type"));
        e.setBasePrice(rs.getDouble("base_price"));
        e.setPriceFloor(rs.getDouble("price_floor"));
        e.setCurrencyCode(rs.getString("currency_code"));
        e.setEffectiveFrom(rs.getString("effective_from"));
        e.setEffectiveTo(rs.getString("effective_to"));
        e.setStatus(rs.getString("status"));
        return e;
    }

    public List<PriceListEntry> findAll() { return query("SELECT * FROM price_list ORDER BY sku_id"); }
    public List<PriceListEntry> findActive() { return query("SELECT * FROM price_list WHERE status='ACTIVE'"); }

    public int insert(PriceListEntry e) {
        return execute(
            "INSERT INTO price_list(price_id,sku_id,region_code,channel,price_type,base_price,price_floor,currency_code,effective_from,effective_to,status) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            e.getPriceId(), e.getSkuId(), e.getRegionCode(), e.getChannel(), e.getPriceType(),
            e.getBasePrice(), e.getPriceFloor(), e.getCurrencyCode(),
            e.getEffectiveFrom(), e.getEffectiveTo(), e.getStatus()
        );
    }

    public int update(PriceListEntry e) {
        return execute(
            "UPDATE price_list SET base_price=?,price_floor=?,status=?,effective_to=? WHERE price_id=?",
            e.getBasePrice(), e.getPriceFloor(), e.getStatus(), e.getEffectiveTo(), e.getPriceId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM price_list WHERE price_id=?", id); }
}
