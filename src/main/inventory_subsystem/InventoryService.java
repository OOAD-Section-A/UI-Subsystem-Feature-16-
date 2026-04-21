package inventory_subsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InventoryService implements InventoryUI {

    private final InventoryDataStore repository;
    private final InventoryExceptionSource exceptionSource;

    private final AddStockStrategy addStrategy = new AddStockStrategy();
    private final RemoveStockStrategy removeStrategy = new RemoveStockStrategy();
    private final TransferStockStrategy transferStrategy = new TransferStockStrategy();

    private IssuingPolicy policy = IssuingPolicy.FIFO;

    public InventoryService(InventoryExceptionSource exceptionSource) {
        this.exceptionSource = exceptionSource;
        this.repository = new InventoryRepository(exceptionSource);
    }

    @Override
    public void addStock(String sku, String locationId,
                         String supplierId, int quantity) {

        addStrategy.execute(
                sku, locationId, supplierId,
                quantity, repository, exceptionSource, policy
        );
    }

    @Override
    public void removeStock(String sku, String locationId,
                            String supplierId, int quantity) {

        removeStrategy.execute(
                sku, locationId, supplierId,
                quantity, repository, exceptionSource, policy
        );
    }

    @Override
    public void transferStock(String sku, String fromLocation,
                              String toLocation, String supplierId, int quantity) {

        transferStrategy.execute(
                sku, fromLocation, supplierId,
                quantity, repository, exceptionSource, policy
        );

        transferStrategy.addToDestination(
                sku, toLocation, supplierId,
                quantity, repository, exceptionSource
        );
    }

    @Override
    public int getStock(String sku, String locationId, String supplierId) {

        InventoryItem item = repository.find(sku, locationId, supplierId);

        if (item == null) {
            exceptionSource.fireResourceNotFound(
                    166, "InventoryItem",
                    sku + "_" + locationId + "_" + supplierId
            );
            return 0;
        }

        return item.getTotalQuantity();
    }

    // Internal read used by facade delta calculations: no exception emission on misses.
    public int peekStock(String sku, String locationId, String supplierId) {
        InventoryItem item = repository.find(sku, locationId, supplierId);
        return item == null ? 0 : item.getTotalQuantity();
    }

    public void addSupplier(String supplierId, String name,
                            int leadTimeDays, double performanceRating) {

        if (repository instanceof InventoryRepository repo) {
            repo.addSupplier(new Supplier(
                    supplierId, name, leadTimeDays, performanceRating
            ));
        }
    }

    public Supplier getSupplier(String supplierId) {

        if (repository instanceof InventoryRepository repo) {
            return repo.getSupplier(supplierId);
        }

        return null;
    }

    public int getTotalStockBySupplier(String supplierId) {

        if (repository instanceof InventoryRepository repo) {
            return repo.getTransactions()
                    .stream()
                    .filter(t -> t.getSupplierId().equals(supplierId))
                    .mapToInt(StockTransaction::getQuantityChange)
                    .sum();
        }

        return 0;
    }

    public void setIssuingPolicy(IssuingPolicy issuingPolicy) {
        if (issuingPolicy != null) this.policy = issuingPolicy;
    }

    public IssuingPolicy getIssuingPolicy() {
        return policy;
    }

    public List<InventoryBatch> getBatches(String sku, String locationId, String supplierId) {
        InventoryItem item = repository.find(sku, locationId, supplierId);
        if (item == null) return Collections.emptyList();
        return new ArrayList<>(item.getBatches());
    }

    public List<StockTransaction> getTransactions() {
        if (repository instanceof InventoryRepository repo) {
            return new ArrayList<>(repo.getTransactions());
        }
        return Collections.emptyList();
    }

    public List<Supplier> getAllSuppliers() {
        if (repository instanceof InventoryRepository repo) {
            return new ArrayList<>(repo.getAllSuppliers());
        }
        return Collections.emptyList();
    }
}
