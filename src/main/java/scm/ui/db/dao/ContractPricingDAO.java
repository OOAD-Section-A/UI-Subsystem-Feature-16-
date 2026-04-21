package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class ContractPricingDAO extends BaseDAO<ContractPricing> {
    public ContractPricingDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected ContractPricing mapRow(ResultSet rs) throws SQLException {
        ContractPricing c = new ContractPricing();
        c.setContractId(rs.getString("contract_id"));
        c.setContractCustomerId(rs.getString("contract_customer_id"));
        c.setContractSkuId(rs.getString("contract_sku_id"));
        c.setNegotiatedPrice(rs.getDouble("negotiated_price"));
        c.setContractStartDate(rs.getString("contract_start_date"));
        c.setContractExpiryDate(rs.getString("contract_expiry_date"));
        c.setContractStatus(rs.getString("contract_status"));
        return c;
    }

    public List<ContractPricing> findAll() { return query("SELECT * FROM contract_pricing ORDER BY contract_start_date DESC"); }

    public int insert(ContractPricing c) {
        return execute(
            "INSERT INTO contract_pricing(contract_id,contract_customer_id,contract_sku_id,negotiated_price,contract_start_date,contract_expiry_date,contract_status) VALUES(?,?,?,?,?,?,?)",
            c.getContractId(), c.getContractCustomerId(), c.getContractSkuId(),
            c.getNegotiatedPrice(), c.getContractStartDate(), c.getContractExpiryDate(), c.getContractStatus()
        );
    }

    public int update(ContractPricing c) {
        return execute(
            "UPDATE contract_pricing SET negotiated_price=?,contract_status=?,contract_expiry_date=? WHERE contract_id=?",
            c.getNegotiatedPrice(), c.getContractStatus(), c.getContractExpiryDate(), c.getContractId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM contract_pricing WHERE contract_id=?", id); }
}
