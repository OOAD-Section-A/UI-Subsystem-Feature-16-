package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class ForecastDAO extends BaseDAO<DemandForecast> {
    public ForecastDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected DemandForecast mapRow(ResultSet rs) throws SQLException {
        DemandForecast d = new DemandForecast();
        d.setForecastId(rs.getString("forecast_id"));
        d.setProductId(rs.getString("product_id"));
        d.setForecastPeriod(rs.getString("forecast_period"));
        d.setForecastDate(rs.getString("forecast_date"));
        d.setPredictedDemand(rs.getInt("predicted_demand"));
        d.setConfidenceScore(rs.getDouble("confidence_score"));
        d.setReorderSignal(rs.getBoolean("reorder_signal"));
        d.setSuggestedOrderQty(rs.getInt("suggested_order_qty"));
        d.setLifecycleStage(rs.getString("lifecycle_stage"));
        d.setAlgorithmUsed(rs.getString("algorithm_used"));
        return d;
    }

    public List<DemandForecast> findAll() { return query("SELECT * FROM demand_forecasts ORDER BY generated_at DESC LIMIT 300"); }
    public List<DemandForecast> findByProduct(String productId) {
        String normalized = productId == null ? "" : productId.trim();
        return query(
            "SELECT * FROM demand_forecasts " +
            "WHERE UPPER(TRIM(product_id)) = UPPER(TRIM(?)) " +
            "   OR UPPER(product_id) LIKE CONCAT('%', UPPER(TRIM(?)), '%') " +
            "ORDER BY forecast_date DESC",
            normalized, normalized
        );
    }

    public int insert(DemandForecast d) {
        return execute(
            "INSERT INTO demand_forecasts(forecast_id,product_id,forecast_period,forecast_date,predicted_demand,confidence_score,reorder_signal,suggested_order_qty,lifecycle_stage,algorithm_used) VALUES(?,?,?,?,?,?,?,?,?,?)",
            d.getForecastId(), d.getProductId(), d.getForecastPeriod(), d.getForecastDate(),
            d.getPredictedDemand(), d.getConfidenceScore(), d.isReorderSignal(),
            d.getSuggestedOrderQty(), d.getLifecycleStage(), d.getAlgorithmUsed()
        );
    }

    public int update(DemandForecast d) {
        return execute(
            "UPDATE demand_forecasts SET predicted_demand=?,confidence_score=?,reorder_signal=?,suggested_order_qty=? WHERE forecast_id=?",
            d.getPredictedDemand(), d.getConfidenceScore(), d.isReorderSignal(), d.getSuggestedOrderQty(), d.getForecastId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM demand_forecasts WHERE forecast_id=?", id); }
}
