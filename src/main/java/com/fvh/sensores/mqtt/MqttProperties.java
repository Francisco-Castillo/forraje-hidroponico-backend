package com.fvh.sensores.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sensores.mqtt")
public record MqttProperties(String url, String username, String password, String clientId, String topic) {
}
