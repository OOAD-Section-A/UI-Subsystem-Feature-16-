package scm.ui.patterns;

import scm.ui.model.BarcodeEvent;
import java.util.UUID;

/**
 * STRUCTURAL PATTERN — ADAPTER
 * BarcodeReaderAdapter adapts raw barcode/RFID scanner input
 * (a plain String from a scanner device) to a BarcodeEvent model
 * that the application understands.
 * Referenced in schema.sql pattern notes.
 */
public class BarcodeReaderAdapter {
    private final RawBarcodeSource source;

    public BarcodeReaderAdapter() {
        this.source = null;
    }

    public BarcodeReaderAdapter(RawBarcodeSource source) {
        this.source = source;
    }

    public interface RawBarcodeSource {
        String readRawBarcode();
        String getDeviceId();
        String getWarehouseId();
    }

    /**
     * Adapts a raw scan string into a BarcodeEvent ready for persistence.
     */
    public BarcodeEvent adapt(String rawScan, String deviceId, String warehouseId, String eventType) {
        BarcodeEvent event = new BarcodeEvent();
        event.setEventId("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        event.setProductId(extractProductId(rawScan));
        event.setEventType(eventType != null ? eventType : "SCAN");
        event.setSourceDevice(deviceId);
        event.setWarehouseId(warehouseId);
        event.setRawPayload(rawScan);
        return event;
    }

    public BarcodeEvent scan(String sku, String locationId, int quantity, String eventType) {
        BarcodeEvent event = adapt(sku, "SIM-SCANNER", locationId, eventType);
        event.setSku(sku);
        event.setQuantity(quantity);
        event.setLocationId(locationId);
        return event;
    }

    /**
     * Adapts a source implementing RawBarcodeSource.
     */
    public BarcodeEvent adaptFromSource(RawBarcodeSource source, String eventType) {
        return adapt(source.readRawBarcode(), source.getDeviceId(), source.getWarehouseId(), eventType);
    }

    public BarcodeEvent scanFromConfiguredSource(String eventType) {
        if (source == null) return null;
        return adaptFromSource(source, eventType);
    }

    private String extractProductId(String rawScan) {
        if (rawScan == null || rawScan.trim().isEmpty()) return "UNKNOWN";
        // Real adapters would parse barcode format (EAN-13, QR, etc.)
        return rawScan.trim().toUpperCase().replaceAll("[^A-Z0-9-]", "");
    }

    public static class SimulatedScanner implements RawBarcodeSource {
        @Override
        public String readRawBarcode() {
            return "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        @Override
        public String getDeviceId() {
            return "SIM-SCANNER";
        }

        @Override
        public String getWarehouseId() {
            return "WH-SIM";
        }
    }
}
