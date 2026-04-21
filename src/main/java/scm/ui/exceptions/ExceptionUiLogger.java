package scm.ui.exceptions;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.db.dao.ExceptionDAO;
import scm.ui.model.SubsystemException;
import scm.ui.patterns.EventBus;

/**
 * Mirrors raised exceptions into the UI exception table.
 * This keeps Notifications → Exceptions in sync even when the handler writes
 * to Windows Event Viewer (which is unavailable on macOS/Linux).
 */
public final class ExceptionUiLogger {

    private ExceptionUiLogger() { }

    public static void logToUiTable(String subsystem,
                                    int exceptionId,
                                    String severity,
                                    String message,
                                    String referenceId) {
        SubsystemException e = new SubsystemException();
        e.setExceptionId("EXC-" + System.currentTimeMillis() + "-" + exceptionId);
        e.setSubsystemCode(subsystem);
        e.setSeverityLevel(severity);
        e.setExceptionMessage(message);
        e.setStatus("OPEN");
        e.setRaisedBy(referenceId == null || referenceId.isBlank() ? "SYSTEM" : referenceId);

        try {
            int rows = new ExceptionDAO(DatabaseConnectionPool.getInstance()).insert(e);
            if (rows > 0) {
                EventBus.getInstance().publish(EventBus.Event.EXCEPTION_RAISED, e);
            }
        } catch (RuntimeException ex) {
            System.err.println("[ExceptionUI] Exception mirror failed: " + ex.getMessage());
        }
    }
}
