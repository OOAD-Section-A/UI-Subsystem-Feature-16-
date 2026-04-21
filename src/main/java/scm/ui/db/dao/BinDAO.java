package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class BinDAO extends BaseDAO<Bin> {
    public BinDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected Bin mapRow(ResultSet rs) throws SQLException {
        Bin b = new Bin();
        b.setBinId(rs.getString("bin_id"));
        b.setZoneId(rs.getString("zone_id"));
        b.setBinCapacity(rs.getInt("bin_capacity"));
        b.setBinStatus(rs.getString("bin_status"));
        return b;
    }

    public List<Bin> findAll() { return query("SELECT * FROM bins ORDER BY bin_id"); }
    public List<Bin> findByZone(String zoneId) { return query("SELECT * FROM bins WHERE zone_id=?", zoneId); }
    public List<Bin> findByWarehouse(String warehouseId) {
        return query(
            "SELECT b.* " +
            "FROM bins b " +
            "JOIN warehouse_zones wz ON wz.zone_id = b.zone_id " +
            "WHERE wz.warehouse_id = ? " +
            "ORDER BY CASE b.bin_status " +
            "  WHEN 'AVAILABLE' THEN 0 " +
            "  WHEN 'OCCUPIED' THEN 1 " +
            "  WHEN 'RESERVED' THEN 2 " +
            "  ELSE 3 END, b.bin_id",
            warehouseId
        );
    }

    public String ensureDefaultBinForWarehouse(String warehouseId) {
        if (warehouseId == null || warehouseId.trim().isEmpty()) return null;

        List<Bin> existing = findByWarehouse(warehouseId);
        if (!existing.isEmpty()) return existing.get(0).getBinId();

        Connection conn = null;
        try {
            conn = pool.getConnection();

            String zoneId = null;
            try (PreparedStatement zonePs = conn.prepareStatement(
                "SELECT zone_id FROM warehouse_zones WHERE warehouse_id = ? " +
                "ORDER BY CASE zone_type " +
                "  WHEN 'STORAGE' THEN 0 " +
                "  WHEN 'PICKING' THEN 1 " +
                "  WHEN 'STAGING' THEN 2 " +
                "  WHEN 'RECEIVING' THEN 3 " +
                "  ELSE 4 END, zone_id LIMIT 1"
            )) {
                zonePs.setString(1, warehouseId);
                try (ResultSet rs = zonePs.executeQuery()) {
                    if (rs.next()) zoneId = rs.getString("zone_id");
                }
            }

            if (zoneId == null || zoneId.trim().isEmpty()) return null;

            String compactWarehouse = warehouseId.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
            String base = ("BIN-AUTO-" + compactWarehouse);
            if (base.length() > 44) base = base.substring(0, 44);

            String binId = base;
            int suffix = 1;
            while (true) {
                try (PreparedStatement existsPs = conn.prepareStatement(
                    "SELECT 1 FROM bins WHERE bin_id = ? LIMIT 1"
                )) {
                    existsPs.setString(1, binId);
                    try (ResultSet rs = existsPs.executeQuery()) {
                        if (!rs.next()) break;
                    }
                }
                binId = base + "-" + suffix++;
            }

            try (PreparedStatement insertPs = conn.prepareStatement(
                "INSERT INTO bins(bin_id, zone_id, bin_capacity, bin_status) VALUES(?,?,?,?)"
            )) {
                insertPs.setString(1, binId);
                insertPs.setString(2, zoneId);
                insertPs.setInt(3, 999999);
                insertPs.setString(4, "AVAILABLE");
                insertPs.executeUpdate();
            }

            return binId;
        } catch (SQLException e) {
            System.err.println("[DAO] ensureDefaultBinForWarehouse error: " + e.getMessage());
            return null;
        } finally {
            pool.releaseConnection(conn);
        }
    }

    public int insert(Bin b) {
        return execute("INSERT INTO bins(bin_id,zone_id,bin_capacity,bin_status) VALUES(?,?,?,?)",
            b.getBinId(), b.getZoneId(), b.getBinCapacity(), b.getBinStatus());
    }

    public int update(Bin b) {
        return execute("UPDATE bins SET bin_capacity=?,bin_status=? WHERE bin_id=?",
            b.getBinCapacity(), b.getBinStatus(), b.getBinId());
    }

    public int delete(String id) { return execute("DELETE FROM bins WHERE bin_id=?", id); }
}
