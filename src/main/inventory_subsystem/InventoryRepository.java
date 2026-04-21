package inventory_subsystem;

import java.util.*;

public class InventoryRepository implements InventoryDataStore {

    private final InventoryExceptionSource exceptionSource;

    private final Map<String, InventoryItem> localStore = new HashMap<>();
    private final List<StockTransaction> transactions = new ArrayList<>();
    private final Map<String, Supplier> suppliers = new HashMap<>();

    public InventoryRepository(InventoryExceptionSource exceptionSource) {
        this.exceptionSource = exceptionSource;
    }

    private String key(String sku, String locationId, String supplierId) {
        return sku + "_" + locationId + "_" + supplierId;
    }

    @Override
    public InventoryItem find(String sku, String locationId, String supplierId) {
        return localStore.get(key(sku, locationId, supplierId));
    }

    @Override
    public void save(InventoryItem item) {
        localStore.put(key(item.getSku(), item.getLocationId(), item.getSupplierId()), item);
    }

    @Override
    public boolean exists(String sku, String locationId, String supplierId) {
        return localStore.containsKey(key(sku, locationId, supplierId));
    }

    public void recordTransaction(StockTransaction tx) {
        transactions.add(tx);
    }

    public List<StockTransaction> getTransactions() {
        return transactions;
    }

    public void addSupplier(Supplier supplier) {
        suppliers.put(supplier.getSupplierId(), supplier);
    }

    public Supplier getSupplier(String supplierId) {
        return suppliers.get(supplierId);
    }

    public Collection<Supplier> getAllSuppliers() {
        return suppliers.values();
    }
}
