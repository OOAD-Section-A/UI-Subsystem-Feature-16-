package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class StockRecordDAO extends BaseDAO<StockRecord> {
    public StockRecordDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected StockRecord mapRow(ResultSet rs) throws SQLException {
        StockRecord r = new StockRecord();
        r.setStockId(rs.getString("stock_id"));
        r.setProductId(rs.getString("product_id"));
        r.setBinId(rs.getString("bin_id"));
        r.setQuantity(rs.getInt("quantity"));
        r.setLastUpdated(rs.getString("last_updated"));
        return r;
    }

    public List<StockRecord> findAll() { return query("SELECT * FROM stock_records ORDER BY product_id"); }
    public StockRecord findByProductAndBin(String productId, String binId) {
        return queryOne("SELECT * FROM stock_records WHERE product_id=? AND bin_id=?", productId, binId);
    }

    public int sumByProduct(String productId) {
        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(quantity),0) AS qty FROM stock_records WHERE product_id=?"
            )) {
                ps.setString(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("qty") : 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] sumByProduct error: " + e.getMessage());
            return 0;
        } finally {
            pool.releaseConnection(conn);
        }
    }

    public int sumByProductAndBin(String productId, String binId) {
        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(quantity),0) AS qty FROM stock_records WHERE product_id=? AND bin_id=?"
            )) {
                ps.setString(1, productId);
                ps.setString(2, binId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("qty") : 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] sumByProductAndBin error: " + e.getMessage());
            return 0;
        } finally {
            pool.releaseConnection(conn);
        }
    }

    public int sumByProductAndWarehouse(String productId, String warehouseId) {
        Connection conn = null;
        try {
            conn = pool.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(sr.quantity),0) AS qty " +
                "FROM stock_records sr " +
                "JOIN bins b ON b.bin_id = sr.bin_id " +
                "JOIN warehouse_zones wz ON wz.zone_id = b.zone_id " +
                "WHERE sr.product_id=? AND wz.warehouse_id=?"
            )) {
                ps.setString(1, productId);
                ps.setString(2, warehouseId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("qty") : 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] sumByProductAndWarehouse error: " + e.getMessage());
            return 0;
        } finally {
            pool.releaseConnection(conn);
        }
    }

    public List<WarehouseProductStock> findWarehouseProductStocks() {
        List<WarehouseProductStock> result = new ArrayList<>();
        Connection conn = null;
        try {
            conn = pool.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT t.warehouse_id, t.bin_id, t.product_id, t.sku_id, t.product_name, SUM(t.total_qty) AS total_qty " +
                "FROM (" +
                "  SELECT wz.warehouse_id AS warehouse_id, sr.bin_id AS bin_id, sr.product_id AS product_id, " +
                "         COALESCE(p.sku, sr.product_id) AS sku_id, COALESCE(p.product_name, COALESCE(p.sku, sr.product_id)) AS product_name, " +
                "         SUM(sr.quantity) AS total_qty " +
                "  FROM stock_records sr " +
                "  JOIN bins b ON b.bin_id = sr.bin_id " +
                "  JOIN warehouse_zones wz ON wz.zone_id = b.zone_id " +
                "  LEFT JOIN products p ON p.product_id = sr.product_id OR p.sku = sr.product_id " +
                "  GROUP BY wz.warehouse_id, sr.bin_id, sr.product_id, COALESCE(p.sku, sr.product_id), COALESCE(p.product_name, COALESCE(p.sku, sr.product_id)) " +
                "  UNION ALL " +
                "  SELECT 'UNMAPPED' AS warehouse_id, " +
                "         'UNMAPPED' AS bin_id, " +
                "         COALESCE(p.product_id, sl.product_id) AS product_id, " +
                "         COALESCE(p.sku, sl.product_id) AS sku_id, " +
                "         COALESCE(p.product_name, COALESCE(p.sku, sl.product_id)) AS product_name, " +
                "         sl.current_stock_qty AS total_qty " +
                "  FROM stock_levels sl " +
                "  LEFT JOIN products p ON p.product_id = sl.product_id OR p.sku = sl.product_id " +
                "  WHERE NOT EXISTS (" +
                "    SELECT 1 FROM stock_records sr2 " +
                "    WHERE sr2.product_id = sl.product_id " +
                "       OR sr2.product_id = COALESCE(p.product_id, sl.product_id)" +
                "  ) " +
                "  UNION ALL " +
                "  SELECT 'UNMAPPED' AS warehouse_id, 'UNMAPPED' AS bin_id, p2.product_id, p2.sku, p2.product_name, 0 AS total_qty " +
                "  FROM products p2 " +
                "  WHERE NOT EXISTS (" +
                "    SELECT 1 FROM stock_records sr3 " +
                "    WHERE sr3.product_id = p2.product_id OR sr3.product_id = p2.sku" +
                "  ) " +
                "    AND NOT EXISTS (" +
                "      SELECT 1 FROM stock_levels sl2 " +
                "      WHERE sl2.product_id = p2.product_id OR sl2.product_id = p2.sku" +
                "    ) " +
                ") t " +
                "GROUP BY t.warehouse_id, t.bin_id, t.product_id, t.sku_id, t.product_name " +
                "ORDER BY CASE WHEN t.warehouse_id='UNMAPPED' THEN 1 ELSE 0 END, t.warehouse_id, t.bin_id, t.product_id"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                WarehouseProductStock row = new WarehouseProductStock();
                row.setWarehouseId(rs.getString("warehouse_id"));
                row.setBinId(rs.getString("bin_id"));
                row.setProductId(rs.getString("product_id"));
                row.setSkuId(rs.getString("sku_id"));
                row.setProductName(rs.getString("product_name"));
                row.setQuantity(rs.getInt("total_qty"));
                result.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Query error: " + e.getMessage());
        } finally {
            pool.releaseConnection(conn);
        }
        return result;
    }

    public int insert(StockRecord r) {
        return execute(
            "INSERT INTO stock_records(stock_id,product_id,bin_id,quantity) VALUES(?,?,?,?)",
            r.getStockId(), r.getProductId(), r.getBinId(), r.getQuantity()
        );
    }

    public int update(StockRecord r) {
        return execute("UPDATE stock_records SET quantity=? WHERE stock_id=?", r.getQuantity(), r.getStockId());
    }

    public int delete(String id) { return execute("DELETE FROM stock_records WHERE stock_id=?", id); }
}
