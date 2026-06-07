package com.hotelos.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Wire format for broker messages. Same shape is used for subscribe, publish,
 * and delivery. The broker reads {@code action} and routes accordingly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrokerMessage {

    public String action;
    public String topic;
    public Map<String, Object> payload;

    public BrokerMessage() {}

    public BrokerMessage(String action, String topic, Map<String, Object> payload) {
        this.action = action;
        this.topic = topic;
        this.payload = payload;
    }

    public static BrokerMessage subscribe(String topic) {
        return new BrokerMessage("subscribe", topic, null);
    }

    public static BrokerMessage publish(String topic, Map<String, Object> payload) {
        return new BrokerMessage("publish", topic, payload);
    }
}
