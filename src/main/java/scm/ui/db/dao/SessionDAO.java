package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class SessionDAO extends BaseDAO<UISession> {
    public SessionDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected UISession mapRow(ResultSet rs) throws SQLException {
        UISession s = new UISession();
        s.setSessionId(rs.getInt("session_id"));
        s.setUserId(rs.getInt("user_id"));
        s.setJwtToken(rs.getString("jwt_session_token"));
        s.setRedirectPanelUrl(rs.getString("redirect_panel_url"));
        s.setSessionExpiryTime(rs.getLong("session_expiry_time"));
        s.setSessionStatus(rs.getString("session_status"));
        return s;
    }

    public long insert(UISession s) {
        return executeAndGetKey(
            "INSERT INTO ui_sessions(user_id,jwt_session_token,redirect_panel_url,session_expiry_time,session_status) VALUES(?,?,?,?,?)",
            s.getUserId(), s.getJwtToken(), s.getRedirectPanelUrl(), s.getSessionExpiryTime(), "ACTIVE"
        );
    }

    public int invalidate(String token) {
        return execute("UPDATE ui_sessions SET session_status='EXPIRED' WHERE jwt_session_token=?", token);
    }

    public UISession findByToken(String token) {
        return queryOne("SELECT * FROM ui_sessions WHERE jwt_session_token=? AND session_status='ACTIVE'", token);
    }

    public int deleteByUser(int userId) {
        return execute("DELETE FROM ui_sessions WHERE user_id=?", userId);
    }
}
