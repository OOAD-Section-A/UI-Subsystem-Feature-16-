package scm.ui.model;

public class ContractPricing {
    private String contractId, contractCustomerId, contractSkuId, contractStartDate, contractExpiryDate, contractStatus;
    private double negotiatedPrice;

    public String getContractId() { return contractId; }
    public void setContractId(String v) { contractId = v; }
    public String getContractCustomerId() { return contractCustomerId; }
    public void setContractCustomerId(String v) { contractCustomerId = v; }
    public String getContractSkuId() { return contractSkuId; }
    public void setContractSkuId(String v) { contractSkuId = v; }
    public double getNegotiatedPrice() { return negotiatedPrice; }
    public void setNegotiatedPrice(double v) { negotiatedPrice = v; }
    public String getContractStartDate() { return contractStartDate; }
    public void setContractStartDate(String v) { contractStartDate = v; }
    public String getContractExpiryDate() { return contractExpiryDate; }
    public void setContractExpiryDate(String v) { contractExpiryDate = v; }
    public String getContractStatus() { return contractStatus; }
    public void setContractStatus(String v) { contractStatus = v; }
}
