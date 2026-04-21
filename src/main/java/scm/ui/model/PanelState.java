package scm.ui.model;

public class PanelState {
    private int panelStateId, userId;
    private String panelId, currentPanelState;

    public int getPanelStateId() { return panelStateId; }
    public void setPanelStateId(int v) { panelStateId = v; }
    public int getUserId() { return userId; }
    public void setUserId(int v) { userId = v; }
    public String getPanelId() { return panelId; }
    public void setPanelId(String v) { panelId = v; }
    public String getCurrentPanelState() { return currentPanelState; }
    public void setCurrentPanelState(String v) { currentPanelState = v; }

    // Compatibility accessors used by facade/navigation
    public String getLastActivePanel() { return panelId; }
    public void setLastActivePanel(String v) {
        panelId = v;
        if (currentPanelState == null) currentPanelState = v;
    }
}
