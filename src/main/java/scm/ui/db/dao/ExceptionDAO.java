package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class ExceptionDAO extends BaseDAO<SubsystemException> {
    public ExceptionDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected SubsystemException mapRow(ResultSet rs) throws SQLException {
        SubsystemException e = new SubsystemException();
        e.setExceptionId(rs.getString("exception_id"));
        e.setSubsystemName(rs.getString("subsystem_name"));
        e.setReferenceId(rs.getString("reference_id"));
        e.setSeverity(rs.getString("severity"));
        e.setExceptionMessage(rs.getString("exception_message"));
        e.setStatus(rs.getString("status"));
        e.setCreatedAt(rs.getString("created_at"));
        e.setResolvedAt(rs.getString("resolved_at"));
        return e;
    }

    public List<SubsystemException> findAll() { return query("SELECT * FROM subsystem_exceptions ORDER BY created_at DESC LIMIT 200"); }
    public List<SubsystemException> findOpen() { return query("SELECT * FROM subsystem_exceptions WHERE status != 'RESOLVED' ORDER BY created_at DESC"); }

    public int insert(SubsystemException e) {
        return execute(
            "INSERT INTO subsystem_exceptions(exception_id,subsystem_name,reference_id,severity,exception_message,status) VALUES(?,?,?,?,?,?)",
            e.getExceptionId(), e.getSubsystemName(), e.getReferenceId(),
            e.getSeverity(), e.getExceptionMessage(), e.getStatus()
        );
    }

    public int resolve(String id) {
        return execute("UPDATE subsystem_exceptions SET status='RESOLVED',resolved_at=NOW() WHERE exception_id=?", id);
    }

    public int delete(String id) { return execute("DELETE FROM subsystem_exceptions WHERE exception_id=?", id); }
}
