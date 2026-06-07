package com.hotelos.common.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.common.events.BrokerMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Thin client that connects a service to the HotelOS broker.
 *
 * Usage:
 *   client.connect();
 *   client.subscribe("room.vacated", payload -> { ... });
 *   client.publish("room.status_changed", Map.of("roomNumber", "204", "status", "DIRTY"));
 *
 * Reconnects on disconnect (broker may not be up yet at service start).
 */
public class BrokerClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final URI brokerUri;
    private final String clientId;
    private final Map<String, List<Consumer<Map<String, Object>>>> handlers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "broker-client-reconnect");
        t.setDaemon(true);
        return t;
    });

    private volatile WebSocketSession session;

    public BrokerClient(String brokerUrl, String clientId) {
        this.brokerUri = URI.create(brokerUrl);
        this.clientId = clientId;
    }

    public void connect() {
        scheduler.schedule(this::tryConnect, 0, TimeUnit.MILLISECONDS);
    }

    private void tryConnect() {
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            client.execute(new InternalHandler(), brokerUri.toString())
                    .whenComplete((s, err) -> {
                        if (err != null) {
                            System.err.println("[" + clientId + "] broker connect failed: " + err.getMessage());
                            scheduler.schedule(this::tryConnect, 2, TimeUnit.SECONDS);
                        } else {
                            session = s;
                            System.out.println("[" + clientId + "] connected to broker");
                            // Re-subscribe to all previously registered topics
                            handlers.keySet().forEach(this::sendSubscribe);
                        }
                    });
        } catch (Exception e) {
            scheduler.schedule(this::tryConnect, 2, TimeUnit.SECONDS);
        }
    }

    public void subscribe(String topic, Consumer<Map<String, Object>> handler) {
        handlers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(handler);
        if (session != null && session.isOpen()) {
            sendSubscribe(topic);
        }
    }

    public void publish(String topic, Map<String, Object> payload) {
        try {
            if (session == null || !session.isOpen()) {
                System.err.println("[" + clientId + "] broker not connected, dropping publish to " + topic);
                return;
            }
            String json = MAPPER.writeValueAsString(BrokerMessage.publish(topic, payload));
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            System.err.println("[" + clientId + "] publish failed: " + e.getMessage());
        }
    }

    private void sendSubscribe(String topic) {
        try {
            String json = MAPPER.writeValueAsString(BrokerMessage.subscribe(topic));
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            System.err.println("[" + clientId + "] subscribe failed: " + e.getMessage());
        }
    }

    private class InternalHandler extends AbstractWebSocketHandler {
        @Override
        protected void handleTextMessage(WebSocketSession ws, TextMessage message) {
            try {
                BrokerMessage msg = MAPPER.readValue(message.getPayload(), BrokerMessage.class);
                if (!"deliver".equals(msg.action)) return;
                List<Consumer<Map<String, Object>>> list = handlers.get(msg.topic);
                if (list == null) return;
                Map<String, Object> payload = msg.payload != null ? msg.payload : new HashMap<>();
                list.forEach(h -> {
                    try { h.accept(payload); }
                    catch (Exception e) { System.err.println("[" + clientId + "] handler error: " + e.getMessage()); }
                });
            } catch (Exception e) {
                System.err.println("[" + clientId + "] message parse failed: " + e.getMessage());
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) {
            session = null;
            System.err.println("[" + clientId + "] broker disconnected, will retry");
            scheduler.schedule(BrokerClient.this::tryConnect, 2, TimeUnit.SECONDS);
        }
    }
}
