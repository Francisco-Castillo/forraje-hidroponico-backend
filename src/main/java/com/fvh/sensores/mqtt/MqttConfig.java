package com.fvh.sensores.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

import com.fvh.sensores.lectura.LecturaService;

@Configuration
@EnableConfigurationProperties(MqttProperties.class)
class MqttConfig {

	@Bean
	MqttPahoClientFactory mqttClientFactory(MqttProperties props) {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setServerURIs(new String[] { props.url() });
		options.setAutomaticReconnect(true);
		// Sesión persistente en el broker: con QoS 1 conserva los mensajes mientras el backend está caído.
		options.setCleanSession(false);
		if (StringUtils.hasText(props.username())) {
			options.setUserName(props.username());
			options.setPassword(props.password().toCharArray());
		}
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		factory.setConnectionOptions(options);
		factory.setPersistence(new MemoryPersistence());
		return factory;
	}

	@Bean
	MessageChannel lecturasMqtt() {
		return new DirectChannel();
	}

	@Bean
	MqttPahoMessageDrivenChannelAdapter lecturasInbound(MqttProperties props, MqttPahoClientFactory factory,
			MessageChannel lecturasMqtt) {
		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(props.clientId(),
				factory, props.topic());
		adapter.setQos(1);
		adapter.setOutputChannel(lecturasMqtt);
		return adapter;
	}

	@ServiceActivator(inputChannel = "lecturasMqtt")
	@Bean
	MessageHandler lecturasHandler(LecturaService service) {
		return (Message<?> message) -> service.recibir(
				message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class), message.getPayload().toString());
	}
}
