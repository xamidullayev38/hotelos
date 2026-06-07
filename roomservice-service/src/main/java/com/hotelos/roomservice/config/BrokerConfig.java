package com.hotelos.roomservice.config;

import com.hotelos.common.broker.BrokerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerConfig {

    @Bean(destroyMethod = "")
    public BrokerClient brokerClient(@Value("${hotelos.broker-url}") String url) {
        BrokerClient client = new BrokerClient(url, "roomservice");
        client.connect();
        return client;
    }
}
