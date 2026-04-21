package scm.ui.db.dao;

import scm.ui.db.DatabaseConnectionPool;
import scm.ui.model.*;
import java.sql.*;
import java.util.*;

public class ProductDAO extends BaseDAO<Product> {
    public ProductDAO(DatabaseConnectionPool p) { super(p); }

    @Override
    protected Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getString("product_id"));
        p.setProductName(rs.getString("product_name"));
        p.setSku(rs.getString("sku"));
        p.setCategory(rs.getString("category"));
        p.setSubCategory(rs.getString("sub_category"));
        p.setSupplierId(rs.getString("supplier_id"));
        p.setUnitOfMeasure(rs.getString("unit_of_measure"));
        p.setStorageConditions(rs.getString("storage_conditions"));
        try { p.setShelfLifeDays(rs.getInt("shelf_life_days")); } catch(Exception ignored){}
        return p;
    }

    public List<Product> findAll() { return query("SELECT * FROM products ORDER BY product_name"); }

    public Product findById(String id) { return queryOne("SELECT * FROM products WHERE product_id=?", id); }

    public List<Product> findByCategory(String cat) {
        return query("SELECT * FROM products WHERE category=? ORDER BY product_name", cat);
    }

    public int insert(Product p) {
        return execute(
            "INSERT INTO products(product_id,product_name,sku,category,sub_category,supplier_id,unit_of_measure,storage_conditions,shelf_life_days) VALUES(?,?,?,?,?,?,?,?,?)",
            p.getProductId(), p.getProductName(), p.getSku(), p.getCategory(),
            p.getSubCategory(), p.getSupplierId(), p.getUnitOfMeasure(),
            p.getStorageConditions(), p.getShelfLifeDays()
        );
    }

    public int update(Product p) {
        return execute(
            "UPDATE products SET product_name=?,sku=?,category=?,sub_category=?,supplier_id=?,unit_of_measure=?,storage_conditions=?,shelf_life_days=? WHERE product_id=?",
            p.getProductName(), p.getSku(), p.getCategory(), p.getSubCategory(),
            p.getSupplierId(), p.getUnitOfMeasure(), p.getStorageConditions(),
            p.getShelfLifeDays(), p.getProductId()
        );
    }

    public int delete(String id) { return execute("DELETE FROM products WHERE product_id=?", id); }
}
