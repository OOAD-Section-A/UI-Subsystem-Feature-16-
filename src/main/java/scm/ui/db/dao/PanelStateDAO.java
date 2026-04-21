package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class PanelStateDAO extends BaseDAO<PanelState> {
    public PanelStateDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected PanelState mapRow(ResultSet rs) throws SQLException {
        PanelState ps = new PanelState();
        ps.setPanelStateId(rs.getInt("panel_state_id"));
        ps.setUserId(rs.getInt("user_id"));
        ps.setPanelId(rs.getString("panel_id"));
        ps.setCurrentPanelState(rs.getString("current_panel_state"));
        return ps;
    }

    public List<PanelState> findByUser(int userId) {
        return query("SELECT * FROM ui_panel_state WHERE user_id=?", userId);
    }

    public int upsert(int userId, String panelId, String state) {
        return execute(
            "INSERT INTO ui_panel_state(user_id,panel_id,current_panel_state) VALUES(?,?,?) ON DUPLICATE KEY UPDATE current_panel_state=?",
            userId, panelId, state, state
        );
    }

    public int upsert(PanelState ps) {
        String state = ps.getCurrentPanelState() != null ? ps.getCurrentPanelState() : ps.getPanelId();
        return upsert(ps.getUserId(), ps.getPanelId(), state);
    }
}
