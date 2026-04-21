package scm.ui.model;

public class UISession {
    private int sessionId, userId;
    private String jwtToken, redirectPanelUrl, sessionStatus;
    private long sessionExpiryTime;

    public int getSessionId() { return sessionId; }
    public void setSessionId(int v) { sessionId = v; }
    public int getUserId() { return userId; }
    public void setUserId(int v) { userId = v; }
    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String v) { jwtToken = v; }
    public String getRedirectPanelUrl() { return redirectPanelUrl; }
    public void setRedirectPanelUrl(String v) { redirectPanelUrl = v; }
    public long getSessionExpiryTime() { return sessionExpiryTime; }
    public void setSessionExpiryTime(long v) { sessionExpiryTime = v; }
    public String getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(String v) { sessionStatus = v; }
}
