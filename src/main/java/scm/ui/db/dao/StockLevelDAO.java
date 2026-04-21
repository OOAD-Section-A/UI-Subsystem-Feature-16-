package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class StockLevelDAO extends BaseDAO<StockLevel> {
    public StockLevelDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected StockLevel mapRow(ResultSet rs) throws SQLException {
        StockLevel s = new StockLevel();
        s.setStockLevelId(rs.getString("stock_level_id"));
        s.setProductId(rs.getString("product_id"));
        s.setCurrentStockQty(rs.getInt("current_stock_qty"));
        s.setReservedStockQty(rs.getInt("reserved_stock_qty"));
        s.setAvailableStockQty(rs.getInt("available_stock_qty"));
        s.setReorderThreshold(rs.getInt("reorder_threshold"));
        s.setReorderQuantity(rs.getInt("reorder_quantity"));
        s.setSafetyStockLevel(rs.getInt("safety_stock_level"));
        return s;
    }

    public List<StockLevel> findAll() { return query("SELECT * FROM stock_levels ORDER BY product_id"); }
    public StockLevel findByProduct(String productId) { return queryOne("SELECT * FROM stock_levels WHERE product_id=?", productId); }
    public List<StockLevel> findLowStock() {
        return query("SELECT * FROM stock_levels WHERE current_stock_qty <= reorder_threshold");
    }

    public int insert(StockLevel s) {
        return execute(
            "INSERT INTO stock_levels(stock_level_id,product_id,current_stock_qty,reserved_stock_qty,available_stock_qty,reorder_threshold,reorder_quantity,safety_stock_level) VALUES(?,?,?,?,?,?,?,?)",
            s.getStockLevelId(), s.getProductId(), s.getCurrentStockQty(), s.getReservedStockQty(),
            s.getAvailableStockQty(), s.getReorderThreshold(), s.getReorderQuantity(), s.getSafetyStockLevel()
        );
    }

    public int update(StockLevel s) {
        return execute(
            "UPDATE stock_levels SET current_stock_qty=?,reserved_stock_qty=?,available_stock_qty=?,reorder_threshold=?,reorder_quantity=?,safety_stock_level=? WHERE stock_level_id=?",
            s.getCurrentStockQty(), s.getReservedStockQty(), s.getAvailableStockQty(),
            s.getReorderThreshold(), s.getReorderQuantity(), s.getSafetyStockLevel(), s.getStockLevelId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM stock_levels WHERE stock_level_id=?", id); }
}
