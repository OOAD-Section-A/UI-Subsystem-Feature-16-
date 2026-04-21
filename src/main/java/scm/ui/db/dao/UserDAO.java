package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class UserDAO extends BaseDAO<UIUser> {
    public UserDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected UIUser mapRow(ResultSet rs) throws SQLException {
        UIUser u = new UIUser();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setUserRole(rs.getString("user_role"));
        u.setAccountLocked(rs.getBoolean("is_account_locked"));
        u.setLoginAttemptCount(rs.getInt("login_attempt_count"));
        u.setEmail(rs.getString("user_email"));
        u.setDisplayName(rs.getString("user_display_name"));
        u.setThemePreference(rs.getString("theme_preference"));
        u.setLanguagePreference(rs.getString("language_preference"));
        u.setLastLoginTimestamp(rs.getString("last_login_timestamp"));
        return u;
    }

    public UIUser findByUsername(String username) {
        return queryOne("SELECT * FROM ui_users WHERE username=?", username);
    }

    public UIUser findByUsernameAndPassword(String username, String passwordHash) {
        return queryOne("SELECT * FROM ui_users WHERE username=? AND password_hash=?", username, passwordHash);
    }

    public List<UIUser> findAll() {
        return query("SELECT * FROM ui_users ORDER BY user_id");
    }

    public long insert(UIUser u) {
        return executeAndGetKey(
            "INSERT INTO ui_users(username,password_hash,user_role,user_email,user_display_name,theme_preference,language_preference) VALUES(?,?,?,?,?,?,?)",
            u.getUsername(), u.getPasswordHash(), u.getUserRole(), u.getEmail(),
            u.getDisplayName(), u.getThemePreference(), u.getLanguagePreference()
        );
    }

    public int update(UIUser u) {
        return execute(
            "UPDATE ui_users SET user_role=?,user_email=?,user_display_name=?,theme_preference=?,language_preference=?,is_account_locked=? WHERE user_id=?",
            u.getUserRole(), u.getEmail(), u.getDisplayName(),
            u.getThemePreference(), u.getLanguagePreference(), u.isAccountLocked(), u.getUserId()
        );
    }

    public int delete(int userId) {
        return execute("DELETE FROM ui_users WHERE user_id=?", userId);
    }

    public int incrementLoginAttempts(int userId) {
        return execute("UPDATE ui_users SET login_attempt_count=login_attempt_count+1 WHERE user_id=?", userId);
    }

    public int lockAccount(int userId, boolean locked) {
        return execute("UPDATE ui_users SET is_account_locked=?,login_attempt_count=0 WHERE user_id=?", locked, userId);
    }

    public int updateLastLogin(int userId) {
        return execute("UPDATE ui_users SET last_login_timestamp=NOW(),login_attempt_count=0 WHERE user_id=?", userId);
    }
}
