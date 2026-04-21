package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * STRUCTURAL PATTERN — TEMPLATE METHOD (base for all DAOs)
 * Provides CRUD scaffolding. Concrete DAOs implement mapRow().
 */
public abstract class BaseDAO<T> {

    protected DatabaseConnectionPool pool;

    public BaseDAO(DatabaseConnectionPool pool) {
        this.pool = pool;
    }

    // ── Abstract ────────────────────────────────────────────────────────────
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    // ── Common query helper ─────────────────────────────────────────────────
    protected List<T> query(String sql, Object... params) {
        List<T> result = new ArrayList<>();
        Connection conn = null;
        try {
            conn = pool.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Query error: " + e.getMessage());
        } finally {
            pool.releaseConnection(conn);
        }
        return result;
    }

    protected int execute(String sql, Object... params) {
        Connection conn = null;
        try {
            conn = pool.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DAO] Execute error: " + e.getMessage());
            return -1;
        } finally {
            pool.releaseConnection(conn);
        }
    }

    protected T queryOne(String sql, Object... params) {
        List<T> list = query(sql, params);
        return list.isEmpty() ? null : list.get(0);
    }

    protected long executeAndGetKey(String sql, Object... params) {
        Connection conn = null;
        try {
            conn = pool.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getLong(1);
        } catch (SQLException e) {
            System.err.println("[DAO] Execute+key error: " + e.getMessage());
        } finally {
            pool.releaseConnection(conn);
        }
        return -1;
    }
}
