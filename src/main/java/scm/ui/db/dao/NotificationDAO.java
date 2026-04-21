package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class NotificationDAO extends BaseDAO<UINotification> {
    public NotificationDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected UINotification mapRow(ResultSet rs) throws SQLException {
        UINotification n = new UINotification();
        n.setNotificationId(rs.getInt("notification_id"));
        n.setUserId(rs.getInt("user_id"));
        n.setNotificationType(rs.getString("notification_type"));
        n.setNotificationMessage(rs.getString("notification_message"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(rs.getString("created_at"));
        return n;
    }

    public List<UINotification> findUnreadByUser(int userId) {
        return query("SELECT * FROM ui_notifications WHERE user_id=? AND is_read=FALSE ORDER BY created_at DESC", userId);
    }

    public List<UINotification> findAllByUser(int userId) {
        return query("SELECT * FROM ui_notifications WHERE user_id=? ORDER BY created_at DESC LIMIT 100", userId);
    }

    public long insert(UINotification n) {
        return executeAndGetKey(
            "INSERT INTO ui_notifications(user_id,notification_type,notification_message) VALUES(?,?,?)",
            n.getUserId(), n.getNotificationType(), n.getNotificationMessage()
        );
    }

    public int markRead(int notificationId) {
        return execute("UPDATE ui_notifications SET is_read=TRUE WHERE notification_id=?", notificationId);
    }

    public int markAllRead(int userId) {
        return execute("UPDATE ui_notifications SET is_read=TRUE WHERE user_id=?", userId);
    }

    public int delete(int notificationId) {
        return execute("DELETE FROM ui_notifications WHERE notification_id=?", notificationId);
    }

    public int countUnread(int userId) {
        List<UINotification> list = query(
            "SELECT notification_id,user_id,notification_type,notification_message,is_read,created_at FROM ui_notifications WHERE user_id=? AND is_read=FALSE", userId);
        return list.size();
    }
}
