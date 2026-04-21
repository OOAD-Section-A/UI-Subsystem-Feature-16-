package scm.ui.patterns;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * BEHAVIOURAL PATTERN — OBSERVER (EventBus)
 * Cross-subsystem event wiring referenced in schema.sql pattern notes.
 * Panels subscribe to events; controllers publish events.
 */
public class EventBus {

    public enum Event {
        USER_LOGGED_IN, USER_LOGGED_OUT, SESSION_EXPIRED,
        PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED,
        STOCK_CHANGED, LOW_STOCK_ALERT,
        ORDER_CREATED, ORDER_UPDATED, ORDER_DELETED,
        SHIPMENT_CREATED, SHIPMENT_UPDATED,
        DELIVERY_STATUS_CHANGED,
        DELIVERY_GPS_UPDATED,
        DELIVERY_POD_SUBMITTED,
        DELIVERY_ETA_UPDATED,
        NOTIFICATION_RECEIVED, NOTIFICATION_READ,
        PRICE_CHANGED, PROMOTION_UPDATED,
        FORECAST_GENERATED,
        AUDIT_LOG_ENTRY,
        EXCEPTION_RAISED, EXCEPTION_RESOLVED,
        PANEL_CHANGED, THEME_CHANGED,
        USER_UPDATED
    }

    private static final EventBus INSTANCE = new EventBus();

    private final Map<Event, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {}

    public static EventBus getInstance() { return INSTANCE; }

    public void subscribe(Event event, Consumer<Object> listener) {
        listeners.computeIfAbsent(event, k -> Collections.synchronizedList(new ArrayList<>())).add(listener);
    }

    public void unsubscribe(Event event, Consumer<Object> listener) {
        List<Consumer<Object>> list = listeners.get(event);
        if (list != null) list.remove(listener);
    }

    public void publish(Event event, Object payload) {
        List<Consumer<Object>> list = listeners.getOrDefault(event, Collections.emptyList());
        for (Consumer<Object> listener : list) {
            try {
                listener.accept(payload);
            } catch (Exception e) {
                System.err.println("[EventBus] Listener error for " + event + ": " + e.getMessage());
            }
        }
    }

    public void publish(Event event) {
        publish(event, null);
    }
}
