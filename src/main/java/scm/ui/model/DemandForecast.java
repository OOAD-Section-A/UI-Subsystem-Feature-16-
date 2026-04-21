package scm.ui.model;

public class DemandForecast {
    private String forecastId, productId, forecastPeriod, forecastDate, lifecycleStage, algorithmUsed;
    private int predictedDemand, suggestedOrderQty;
    private double confidenceScore;
    private boolean reorderSignal;

    public String getForecastId() { return forecastId; }
    public void setForecastId(String v) { forecastId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { productId = v; }
    public String getForecastPeriod() { return forecastPeriod; }
    public void setForecastPeriod(String v) { forecastPeriod = v; }
    public String getForecastDate() { return forecastDate; }
    public void setForecastDate(String v) { forecastDate = v; }
    public int getPredictedDemand() { return predictedDemand; }
    public void setPredictedDemand(int v) { predictedDemand = v; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double v) { confidenceScore = v; }
    public boolean isReorderSignal() { return reorderSignal; }
    public void setReorderSignal(boolean v) { reorderSignal = v; }
    public int getSuggestedOrderQty() { return suggestedOrderQty; }
    public void setSuggestedOrderQty(int v) { suggestedOrderQty = v; }
    public String getLifecycleStage() { return lifecycleStage; }
    public void setLifecycleStage(String v) { lifecycleStage = v; }
    public String getAlgorithmUsed() { return algorithmUsed; }
    public void setAlgorithmUsed(String v) { algorithmUsed = v; }

    // Compatibility accessors used by UI panels
    public String getForecastPeriodStart() { return forecastPeriod; }
    public void setForecastPeriodStart(String v) { forecastPeriod = v; }
    public String getForecastPeriodEnd() { return forecastDate; }
    public void setForecastPeriodEnd(String v) { forecastDate = v; }
    public String getForecastAlgorithm() { return algorithmUsed; }
    public void setForecastAlgorithm(String v) { algorithmUsed = v; }
    public int getForecastedQty() { return predictedDemand; }
    public void setForecastedQty(int v) { predictedDemand = v; }
    public boolean isReorderSuggested() { return reorderSignal; }
    public void setReorderSuggested(boolean v) { reorderSignal = v; }
    public String getCreatedBy() { return lifecycleStage; }
    public void setCreatedBy(String v) { lifecycleStage = v; }
}
