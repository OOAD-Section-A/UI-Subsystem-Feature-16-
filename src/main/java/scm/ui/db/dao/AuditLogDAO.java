package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class AuditLogDAO extends BaseDAO<AuditLog> {
    public AuditLogDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setAuditId(rs.getInt("audit_id"));
        a.setAuditTimestamp(rs.getString("audit_timestamp"));
        a.setActionUser(rs.getString("audit_action_user"));
        a.setActionDescription(rs.getString("audit_action_description"));
        a.setModuleName(rs.getString("audit_module_name"));
        return a;
    }

    public List<AuditLog> findAll(int limit) {
        return query("SELECT * FROM ui_audit_log ORDER BY audit_timestamp DESC LIMIT ?", limit);
    }

    public List<AuditLog> findByModule(String module) {
        return query("SELECT * FROM ui_audit_log WHERE audit_module_name=? ORDER BY audit_timestamp DESC", module);
    }

    public long insert(AuditLog a) {
        return executeAndGetKey(
            "INSERT INTO ui_audit_log(audit_action_user,audit_action_description,audit_module_name) VALUES(?,?,?)",
            a.getActionUser(), a.getActionDescription(), a.getModuleName()
        );
    }
}
