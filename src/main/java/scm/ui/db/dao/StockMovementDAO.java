package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class StockMovementDAO extends BaseDAO<StockMovement> {
    public StockMovementDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected StockMovement mapRow(ResultSet rs) throws SQLException {
        StockMovement m = new StockMovement();
        m.setMovementId(rs.getString("movement_id"));
        m.setMovementType(rs.getString("movement_type"));
        m.setFromBin(rs.getString("from_bin"));
        m.setToBin(rs.getString("to_bin"));
        m.setProductId(rs.getString("product_id"));
        m.setMovedQty(rs.getInt("moved_qty"));
        m.setMovementTs(rs.getString("movement_ts"));
        return m;
    }

    public List<StockMovement> findAll() { return query("SELECT * FROM stock_movements ORDER BY movement_ts DESC LIMIT 200"); }
    public List<StockMovement> findByProduct(String pid) { return query("SELECT * FROM stock_movements WHERE product_id=? ORDER BY movement_ts DESC", pid); }

    public int insert(StockMovement m) {
        return execute(
            "INSERT INTO stock_movements(movement_id,movement_type,from_bin,to_bin,product_id,moved_qty) VALUES(?,?,?,?,?,?)",
            m.getMovementId(), m.getMovementType(), m.getFromBin(), m.getToBin(), m.getProductId(), m.getMovedQty()
        );
    }

    public int delete(String id) { return execute("DELETE FROM stock_movements WHERE movement_id=?", id); }
}
