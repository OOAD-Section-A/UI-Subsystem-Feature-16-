package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class ShipmentDAO extends BaseDAO<Shipment> {
    public ShipmentDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected Shipment mapRow(ResultSet rs) throws SQLException {
        Shipment s = new Shipment();
        s.setShipmentId(rs.getString("shipment_id"));
        s.setOrderId(rs.getString("order_id"));
        s.setOriginAddress(rs.getString("origin_address"));
        s.setDestinationAddress(rs.getString("destination_address"));
        s.setPackageWeight(rs.getDouble("package_weight"));
        s.setShippingPriority(rs.getString("shipping_priority"));
        s.setShipmentStatus(rs.getString("shipment_status"));
        s.setCarrierId(rs.getString("carrier_id"));
        s.setTrackingId(rs.getString("tracking_id"));
        s.setCalculatedCost(rs.getDouble("calculated_cost"));
        return s;
    }

    public List<Shipment> findAll() { return query("SELECT * FROM shipments ORDER BY created_at DESC LIMIT 200"); }
    public List<Shipment> findByStatus(String status) { return query("SELECT * FROM shipments WHERE shipment_status=?", status); }
    public List<ShipmentMapPoint> findLiveMapPoints() {
        List<ShipmentMapPoint> points = new ArrayList<>();
        Connection conn = null;
        try {
            conn = pool.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT s.shipment_id, s.shipment_status, s.origin_address, s.destination_address, t.vehicle_id, t.gps_coordinates " +
                "FROM shipments s " +
                "LEFT JOIN (" +
                "  SELECT d.order_id, e.vehicle_id, e.gps_coordinates, e.event_timestamp " +
                "  FROM delivery_orders d " +
                "  JOIN delivery_tracking_events e ON e.delivery_id = d.delivery_id " +
                "  JOIN (" +
                "    SELECT d2.order_id, MAX(e2.event_timestamp) AS latest_ts " +
                "    FROM delivery_orders d2 " +
                "    JOIN delivery_tracking_events e2 ON e2.delivery_id = d2.delivery_id " +
                "    WHERE e2.gps_coordinates IS NOT NULL AND TRIM(e2.gps_coordinates) <> '' " +
                "    GROUP BY d2.order_id" +
                "  ) latest ON latest.order_id = d.order_id AND latest.latest_ts = e.event_timestamp " +
                ") t ON t.order_id = s.order_id " +
                "WHERE UPPER(TRIM(s.shipment_status)) IN ('IN_TRANSIT', 'DISPATCHED') " +
                "ORDER BY s.created_at DESC LIMIT 300"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String coords = rs.getString("gps_coordinates");
                boolean liveGps = false;
                double lat = 0.0;
                double lng = 0.0;
                if (coords != null && !coords.trim().isEmpty()) {
                    String[] parts = coords.split(",");
                    if (parts.length == 2) {
                        try {
                            lat = Double.parseDouble(parts[0].trim());
                            lng = Double.parseDouble(parts[1].trim());
                            liveGps = lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
                        } catch (NumberFormatException ignored) {
                            liveGps = false;
                        }
                    }
                }

                ShipmentMapPoint point = new ShipmentMapPoint();
                point.setShipmentId(rs.getString("shipment_id"));
                point.setShipmentStatus(rs.getString("shipment_status"));
                point.setVehicleId(rs.getString("vehicle_id"));
                point.setOriginAddress(rs.getString("origin_address"));
                point.setDestinationAddress(rs.getString("destination_address"));
                point.setLatitude(lat);
                point.setLongitude(lng);
                point.setLiveGpsAvailable(liveGps);
                points.add(point);
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Shipment live-map query error: " + e.getMessage());
        } finally {
            pool.releaseConnection(conn);
        }
        return points;
    }

    public int insert(Shipment s) {
        return execute(
            "INSERT INTO shipments(shipment_id,order_id,origin_address,destination_address,package_weight,shipping_priority,shipment_status,carrier_id,tracking_id,calculated_cost) VALUES(?,?,?,?,?,?,?,?,?,?)",
            s.getShipmentId(), s.getOrderId(), s.getOriginAddress(), s.getDestinationAddress(),
            s.getPackageWeight(), s.getShippingPriority(), s.getShipmentStatus(),
            s.getCarrierId(), s.getTrackingId(), s.getCalculatedCost()
        );
    }

    public int update(Shipment s) {
        return execute(
            "UPDATE shipments SET shipment_status=?,carrier_id=?,tracking_id=?,calculated_cost=? WHERE shipment_id=?",
            s.getShipmentStatus(), s.getCarrierId(), s.getTrackingId(), s.getCalculatedCost(), s.getShipmentId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM shipments WHERE shipment_id=?", id); }
}
