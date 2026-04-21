package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class WarehouseDAO extends BaseDAO<Warehouse> {
    public WarehouseDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected Warehouse mapRow(ResultSet rs) throws SQLException {
        Warehouse w = new Warehouse();
        w.setWarehouseId(rs.getString("warehouse_id"));
        w.setWarehouseName(rs.getString("warehouse_name"));
        return w;
    }

    public List<Warehouse> findAll() { return query("SELECT * FROM warehouses ORDER BY warehouse_name"); }

    public int insert(Warehouse w) {
        return execute("INSERT INTO warehouses(warehouse_id,warehouse_name) VALUES(?,?)", w.getWarehouseId(), w.getWarehouseName());
    }

    public int update(Warehouse w) {
        return execute("UPDATE warehouses SET warehouse_name=? WHERE warehouse_id=?", w.getWarehouseName(), w.getWarehouseId());
    }

    public int delete(String id) { return execute("DELETE FROM warehouses WHERE warehouse_id=?", id); }
}
