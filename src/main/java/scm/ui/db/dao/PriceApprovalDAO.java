package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class PriceApprovalDAO extends BaseDAO<PriceApproval> {
    public PriceApprovalDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected PriceApproval mapRow(ResultSet rs) throws SQLException {
        PriceApproval a = new PriceApproval();
        a.setApprovalId(rs.getString("approval_id"));
        a.setRequestType(rs.getString("request_type"));
        a.setRequestedBy(rs.getString("requested_by"));
        a.setRequestedDiscountAmt(rs.getDouble("requested_discount_amt"));
        a.setJustificationText(rs.getString("justification_text"));
        a.setApprovingManagerId(rs.getString("approving_manager_id"));
        a.setApprovalStatus(rs.getString("approval_status"));
        a.setCreatedAt(rs.getString("created_at"));
        return a;
    }

    public List<PriceApproval> findAll() { return query("SELECT * FROM price_approvals ORDER BY created_at DESC"); }
    public List<PriceApproval> findPending() { return query("SELECT * FROM price_approvals WHERE approval_status='PENDING'"); }

    public int insert(PriceApproval a) {
        return execute(
            "INSERT INTO price_approvals(approval_id,request_type,requested_by,requested_discount_amt,justification_text,approval_status) VALUES(?,?,?,?,?,?)",
            a.getApprovalId(), a.getRequestType(), a.getRequestedBy(),
            a.getRequestedDiscountAmt(), a.getJustificationText(), "PENDING"
        );
    }

    public int updateStatus(String approvalId, String status, String managerId) {
        return execute(
            "UPDATE price_approvals SET approval_status=?,approving_manager_id=?,approval_timestamp=NOW() WHERE approval_id=?",
            status, managerId, approvalId
        );
    }

    public int delete(String id) { return execute("DELETE FROM price_approvals WHERE approval_id=?", id); }
}
