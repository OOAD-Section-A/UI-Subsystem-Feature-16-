package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class BarcodeEventDAO extends BaseDAO<BarcodeEvent> {
    public BarcodeEventDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected BarcodeEvent mapRow(ResultSet rs) throws SQLException {
        BarcodeEvent b = new BarcodeEvent();
        b.setEventId(rs.getString("event_id"));
        b.setProductId(rs.getString("product_id"));
        b.setEventType(rs.getString("event_type"));
        b.setSourceDevice(rs.getString("source_device"));
        b.setWarehouseId(rs.getString("warehouse_id"));
        b.setEventTimestamp(rs.getString("event_timestamp"));
        b.setRawPayload(rs.getString("raw_payload"));
        return b;
    }

    public List<BarcodeEvent> findAll() { return query("SELECT * FROM barcode_rfid_events ORDER BY event_timestamp DESC LIMIT 200"); }

    public int insert(BarcodeEvent b) {
        return execute(
            "INSERT INTO barcode_rfid_events(event_id,product_id,event_type,source_device,warehouse_id,event_timestamp,raw_payload) VALUES(?,?,?,?,?,NOW(),?)",
            b.getEventId(), b.getProductId(), b.getEventType(), b.getSourceDevice(), b.getWarehouseId(), b.getRawPayload()
        );
    }

    public int delete(String id) { return execute("DELETE FROM barcode_rfid_events WHERE event_id=?", id); }
}
