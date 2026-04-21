package scm.ui.exceptions;

import com.scm.core.SCMException;
import com.scm.factory.SCMExceptionFactory;
import com.scm.handler.SCMExceptionHandler;
import com.scm.subsystems.UISubsystem;

/**
 * UI exception bridge for the updated Exception Handler subsystem.
 * This class intentionally routes only UI/Dashboard exceptions.
 */
public final class UIAuthExceptionSource {

    private static final String SUBSYSTEM = "UI/Dashboard (T3N50R)";
    private static volatile UIAuthExceptionSource instance;
    private final UISubsystem exceptions = UISubsystem.INSTANCE;

    private UIAuthExceptionSource() { }

    public static UIAuthExceptionSource getInstance() {
        if (instance == null) {
            synchronized (UIAuthExceptionSource.class) {
                if (instance == null) {
                    instance = new UIAuthExceptionSource();
                }
            }
        }
        return instance;
    }

    // Kept for backward compatibility with existing startup wiring.
    public void registerHandler(Object ignored) { }

    private void fireUnregistered(String message) {
        ExceptionUiLogger.logToUiTable("UI/Dashboard", 0, "MINOR", message, "SYSTEM");
        SCMException ex = SCMExceptionFactory.createUnregistered(SUBSYSTEM, message);
        SCMExceptionHandler.INSTANCE.handle(ex);
    }

    public void fireAuthFailed(int exceptionId,
                               String userId,
                               String ipAddress,
                               String reason) {
        switch (exceptionId) {
            case 252 -> {
                ExceptionUiLogger.logToUiTable("UI/Dashboard", 252, "MAJOR",
                        "INVALID_CREDENTIALS userId=" + userId + " ip=" + ipAddress + " reason=" + reason,
                        userId);
                exceptions.onInvalidCredentials(userId, ipAddress);
            }
            case 255 -> {
                ExceptionUiLogger.logToUiTable("UI/Dashboard", 255, "MAJOR",
                        "INVALID_2FA_TOKEN userId=" + userId + " ip=" + ipAddress + " reason=" + reason,
                        userId);
                exceptions.onInvalid2faToken(userId, ipAddress);
            }
            // 251 (DB_AUTHENTICATION_FAILED) belongs to Database Design subsystem, not UI.
            case 251 -> fireUnregistered(
                    "DB_AUTHENTICATION_FAILED routed from UI login: userId=" + userId
                            + " ip=" + ipAddress + " reason=" + reason
            );
            default -> fireUnregistered(
                    "fireAuthFailed unknown id=" + exceptionId + " userId=" + userId
                            + " ip=" + ipAddress + " reason=" + reason
            );
        }
    }

    public void fireUnauthorizedAccess(int exceptionId,
                                       String userId,
                                       String resource,
                                       String ipAddress) {
        if (exceptionId == 254) {
            ExceptionUiLogger.logToUiTable("UI/Dashboard", 254, "MAJOR",
                    "UNAUTHORIZED_ACCESS userId=" + userId + " resource=" + resource + " ip=" + ipAddress,
                    userId);
            exceptions.onUnauthorizedAccess(userId, resource, ipAddress);
            return;
        }
        fireUnregistered(
                "fireUnauthorizedAccess unknown id=" + exceptionId + " userId=" + userId
                        + " resource=" + resource + " ip=" + ipAddress
        );
    }

    public void fireSessionEvent(int exceptionId,
                                 String userId,
                                 String sessionId) {
        if (exceptionId == 253) {
            ExceptionUiLogger.logToUiTable("UI/Dashboard", 253, "MAJOR",
                    "SESSION_EXPIRED userId=" + userId + " sessionId=" + sessionId,
                    userId);
            exceptions.onSessionExpired(userId, sessionId);
            return;
        }
        fireUnregistered(
                "fireSessionEvent unknown id=" + exceptionId + " userId=" + userId
                        + " sessionId=" + sessionId
        );
    }

    public void fireAccountLocked(int exceptionId,
                                  String userId,
                                  int failedAttempts) {
        if (exceptionId == 256) {
            ExceptionUiLogger.logToUiTable("UI/Dashboard", 256, "MAJOR",
                    "ACCOUNT_LOCKED userId=" + userId + " failedAttempts=" + failedAttempts,
                    userId);
            exceptions.onAccountLocked(userId, failedAttempts);
            return;
        }
        fireUnregistered(
                "fireAccountLocked unknown id=" + exceptionId + " userId=" + userId
                        + " failedAttempts=" + failedAttempts
        );
    }

    public void firePermissionError(int exceptionId,
                                    String userId,
                                    String operation,
                                    String reason) {
        switch (exceptionId) {
            case 257 -> {
                ExceptionUiLogger.logToUiTable("UI/Dashboard", 257, "MAJOR",
                        "ROLE_ASSIGNMENT_ERROR userId=" + userId + " operation=" + operation + " reason=" + reason,
                        userId);
                exceptions.onRoleAssignmentError(userId, operation, reason);
            }
            case 258 -> {
                ExceptionUiLogger.logToUiTable("UI/Dashboard", 258, "MAJOR",
                        "SETTINGS_SAVE_FAILURE userId=" + userId + " operation=" + operation + " reason=" + reason,
                        userId);
                exceptions.onSettingsSaveFailure(userId);
            }
            default -> fireUnregistered(
                    "firePermissionError unknown id=" + exceptionId + " userId=" + userId
                            + " operation=" + operation + " reason=" + reason
            );
        }
    }

    public void fireVisualizationError(String chartId, String reason) {
        ExceptionUiLogger.logToUiTable("UI/Dashboard", 354, "MINOR",
                "VISUALIZATION_RENDER_ERROR chartId=" + chartId + " reason=" + reason,
                chartId);
        exceptions.onVisualizationRenderError(chartId, reason);
    }
}
