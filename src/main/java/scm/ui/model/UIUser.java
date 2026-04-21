package scm.ui.model;

import java.io.Serializable;

public class UIUser implements Serializable {
    private int userId;
    private String username;
    private String passwordHash;
    private String userRole;
    private boolean accountLocked;
    private int loginAttemptCount;
    private String email;
    private String displayName;
    private String status = "ACTIVE";
    private String themePreference = "LIGHT";
    private String languagePreference = "en";
    private String lastLoginTimestamp;

    public int getUserId() { return userId; }
    public void setUserId(int v) { userId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { username = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { passwordHash = v; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String v) { userRole = v; }
    public boolean isAccountLocked() { return accountLocked; }
    public void setAccountLocked(boolean v) { accountLocked = v; }
    public int getLoginAttemptCount() { return loginAttemptCount; }
    public void setLoginAttemptCount(int v) { loginAttemptCount = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { displayName = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getThemePreference() { return themePreference; }
    public void setThemePreference(String v) { themePreference = v; }
    public String getLanguagePreference() { return languagePreference; }
    public void setLanguagePreference(String v) { languagePreference = v; }
    public String getLastLoginTimestamp() { return lastLoginTimestamp; }
    public void setLastLoginTimestamp(String v) { lastLoginTimestamp = v; }
    public String getLastLogin() { return lastLoginTimestamp; }
    public void setLastLogin(String v) { lastLoginTimestamp = v; }
    @Override public String toString() { return displayName + " [" + userRole + "]"; }
}
