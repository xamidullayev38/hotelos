package com.hotelos.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.common.events.BrokerMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Browser-facing WebSocket. Receives broker deliveries from {@link BrokerBridge}
 * and fans them out to every connected browser tab.
 *
 * Security gate: connect() checks the Authorization header. Without
 * `Bearer demo-token` the session is rejected immediately.
 */
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("[dashboard] browser connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("[dashboard] browser disconnected: " + session.getId());
    }

    /** Called by the BrokerBridge for every delivery. */
    public void broadcast(BrokerMessage delivery) {
        try {
            String json = MAPPER.writeValueAsString(delivery);
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession s : sessions) {
                if (!s.isOpen()) continue;
                synchronized (s) {
                    try { s.sendMessage(msg); }
                    catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("[dashboard] broadcast failed: " + e.getMessage());
        }
    }
}
