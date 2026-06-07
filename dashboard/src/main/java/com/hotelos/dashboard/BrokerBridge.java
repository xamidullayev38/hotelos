package com.hotelos.dashboard;

import com.hotelos.common.broker.BrokerClient;
import com.hotelos.common.events.BrokerMessage;
import com.hotelos.common.events.Topics;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Subscribes to every broker topic the dashboard cares about and forwards
 * each delivery to the browser-facing WebSocket. Decouples the broker
 * client (Spring WS client) from the browser-facing handler (Spring WS
 * server) — they are intentionally different connections.
 */
@Component
public class BrokerBridge {

    private final BrokerClient broker;
    private final DashboardWebSocketHandler dashboard;

    public BrokerBridge(@Value("${hotelos.broker-url}") String brokerUrl,
                        DashboardWebSocketHandler dashboard) {
        this.dashboard = dashboard;
        this.broker = new BrokerClient(brokerUrl, "dashboard");
        this.broker.connect();
    }

    @PostConstruct
    void subscribeAll() {
        String[] topics = {
                Topics.ROOM_VACATED,
                Topics.ROOM_STATUS_CHANGED,
                Topics.ORDER_CREATED,
                Topics.ORDER_STATUS_CHANGED,
                Topics.MAINTENANCE_REPORTED,
                Topics.MAINTENANCE_RESOLVED
        };
        for (String topic : topics) {
            broker.subscribe(topic, payload ->
                    dashboard.broadcast(new BrokerMessage("deliver", topic, payload))
            );
        }
    }
}
