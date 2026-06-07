package com.hotelos.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.common.events.BrokerMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heart of the message broker. Holds a topic -> set of sessions map.
 *
 *   subscribe   adds session to topic's set
 *   publish     fan-outs to every session in topic's set (except sender)
 *
 * Synchronisation: the topic map is concurrent; per-topic session set is
 * also concurrent so subscribe/publish can race safely. Send to closed
 * sessions is guarded.
 */
public class PubSubWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Set<WebSocketSession>> topics = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("[broker] client connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            BrokerMessage msg = MAPPER.readValue(message.getPayload(), BrokerMessage.class);
            if (msg.action == null || msg.topic == null) {
                return;
            }
            switch (msg.action) {
                case "subscribe" -> {
                    topics.computeIfAbsent(msg.topic, k -> ConcurrentHashMap.newKeySet()).add(session);
                    System.out.println("[broker] " + session.getId() + " subscribed to " + msg.topic);
                }
                case "publish" -> {
                    Set<WebSocketSession> subscribers = topics.get(msg.topic);
                    if (subscribers == null) return;
                    String deliveryJson = MAPPER.writeValueAsString(
                            new BrokerMessage("deliver", msg.topic, msg.payload)
                    );
                    TextMessage delivery = new TextMessage(deliveryJson);
                    int fanout = 0;
                    for (WebSocketSession sub : subscribers) {
                        if (sub.isOpen()) {
                            synchronized (sub) {
                                try { sub.sendMessage(delivery); fanout++; }
                                catch (Exception e) { /* drop on broken pipe */ }
                            }
                        }
                    }
                    System.out.println("[broker] published " + msg.topic + " to " + fanout + " subscribers");
                }
                default -> System.err.println("[broker] unknown action: " + msg.action);
            }
        } catch (Exception e) {
            System.err.println("[broker] message handling failed: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        topics.values().forEach(set -> set.remove(session));
        System.out.println("[broker] client disconnected: " + session.getId());
    }
}
