package com.hotelos.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
@EnableWebSocket
public class BrokerApplication implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(BrokerApplication.class, args);
    }

    @Bean
    public PubSubWebSocketHandler pubSubHandler() {
        return new PubSubWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pubSubHandler(), "/broker").setAllowedOrigins("*");
    }
}
