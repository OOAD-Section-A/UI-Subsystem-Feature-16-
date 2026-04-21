package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class PromotionDAO extends BaseDAO<Promotion> {
    public PromotionDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected Promotion mapRow(ResultSet rs) throws SQLException {
        Promotion p = new Promotion();
        p.setPromoId(rs.getString("promo_id"));
        p.setPromoName(rs.getString("promo_name"));
        p.setCouponCode(rs.getString("coupon_code"));
        p.setDiscountType(rs.getString("discount_type"));
        p.setDiscountValue(rs.getDouble("discount_value"));
        p.setStartDate(rs.getString("start_date"));
        p.setEndDate(rs.getString("end_date"));
        p.setMinCartValue(rs.getDouble("min_cart_value"));
        p.setMaxUses(rs.getInt("max_uses"));
        p.setCurrentUseCount(rs.getInt("current_use_count"));
        return p;
    }

    public List<Promotion> findAll() { return query("SELECT * FROM promotions ORDER BY start_date DESC"); }

    public int insert(Promotion p) {
        return execute(
            "INSERT INTO promotions(promo_id,promo_name,coupon_code,discount_type,discount_value,start_date,end_date,eligible_sku_ids,min_cart_value,max_uses,current_use_count) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            p.getPromoId(), p.getPromoName(), p.getCouponCode(), p.getDiscountType(),
            p.getDiscountValue(), p.getStartDate(), p.getEndDate(),
            "[]", p.getMinCartValue(), p.getMaxUses(), 0
        );
    }

    public int update(Promotion p) {
        return execute(
            "UPDATE promotions SET promo_name=?,coupon_code=?,discount_type=?,discount_value=?,start_date=?,end_date=?,min_cart_value=?,max_uses=? WHERE promo_id=?",
            p.getPromoName(), p.getCouponCode(), p.getDiscountType(), p.getDiscountValue(),
            p.getStartDate(), p.getEndDate(), p.getMinCartValue(), p.getMaxUses(), p.getPromoId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM promotions WHERE promo_id=?", id); }
}
