package scm.ui.model;

public class UINotification {
    private int notificationId, userId;
    private String notificationType, notificationMessage, createdAt;
    private boolean read;

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int v) { notificationId = v; }
    public int getUserId() { return userId; }
    public void setUserId(int v) { userId = v; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String v) { notificationType = v; }
    public String getNotificationMessage() { return notificationMessage; }
    public void setNotificationMessage(String v) { notificationMessage = v; }
    public boolean isRead() { return read; }
    public void setRead(boolean v) { read = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { createdAt = v; }
    @Override public String toString() { return "[" + notificationType + "] " + notificationMessage; }
}
