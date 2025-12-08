package com.azziedevelopment.ai2mqtt.service;

import com.azziedevelopment.ai2mqtt.dto.AIRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "messaging.type", havingValue = "mqtt")
public class MQTTAdapter implements MessagingService {

	// We inject the handler defined in the inner config below
	// This bypasses the "Channel" entirely for outbound messages
	@Autowired
	@Qualifier("mqttOutboundHandler")
	private MessageHandler mqttOutboundHandler;

	@Override
	public void sendResponse(String correlationId, String threadId, String responseText) {
		String jsonPayload = """
			{
			    "id": "%s",
			    "threadId": "%s",
			    "response": "%s"
			}
			""".formatted(correlationId, threadId,
			responseText.replace("\"", "\\\"").replace("\n", "\\n"));

		Message<String> message = MessageBuilder.withPayload(jsonPayload).build();

		// DIRECT CALL: No channel, no dispatcher, no "missing subscriber" errors.
		mqttOutboundHandler.handleMessage(message);

		log.info("Sent MQTT Response [ID: {}]", correlationId);
	}

	/**
	 * INNER CONFIGURATION CLASS
	 * Isolates the Bean Definitions so they are fully initialized before the Service uses them.
	 */
	@Configuration
	@ConditionalOnProperty(name = "messaging.type", havingValue = "mqtt")
	@RequiredArgsConstructor
	public static class MqttConfiguration {

		private final OpenAIService aiService;
		private final ObjectMapper objectMapper;

		// --- CONNECTION PROPERTIES (Moved here) ---
		@Value("${mqtt.broker.url}")
		private String brokerUrl;

		@Value("${mqtt.username:}")
		private String username;

		@Value("${mqtt.password:}")
		private String password;

		@Value("${mqtt.client.id}")
		private String clientId;

		@Value("${mqtt.topic.prompts}")
		private String promptTopic;

		@Value("${mqtt.topic.responses}")
		private String responseTopic;

		// --- 1. FACTORY ---
		@Bean
		public MqttPahoClientFactory mqttClientFactory() {
			DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
			MqttConnectOptions options = new MqttConnectOptions();
			options.setServerURIs(new String[]{brokerUrl});

			if (username != null && !username.isBlank()) {
				options.setUserName(username);
				options.setPassword(password.toCharArray());
			}

			options.setCleanSession(true);
			factory.setConnectionOptions(options);
			return factory;
		}

		// --- 2. INBOUND (Listener) ---
		@Bean
		public MessageChannel mqttInputChannel() {
			return new DirectChannel();
		}

		@Bean
		public MqttPahoMessageDrivenChannelAdapter inboundAdapter(MqttPahoClientFactory clientFactory) {
			String listenerId = clientId + "-listener";
			MqttPahoMessageDrivenChannelAdapter adapter =
				new MqttPahoMessageDrivenChannelAdapter(listenerId, clientFactory, promptTopic);

			adapter.setCompletionTimeout(5000);
			adapter.setConverter(new DefaultPahoMessageConverter());
			adapter.setOutputChannel(mqttInputChannel());
			return adapter;
		}

		@ServiceActivator(inputChannel = "mqttInputChannel")
		public void handleMQTTMessage(Message<String> message) {
			String payload = message.getPayload();
			log.debug("Received MQTT Payload: {}", payload);

			try {
				AIRequest request = objectMapper.readValue(payload, AIRequest.class);
				String id = (request.id() != null) ? request.id() : UUID.randomUUID().toString();

				aiService.processPrompt(id, request.threadId(), request.text(), request.systemPrompt());

			} catch (Exception e) {
				String id = UUID.randomUUID().toString();
				aiService.processPrompt(id, null, payload, null);
			}
		}

		// --- 3. OUTBOUND (Sender Bean) ---
		// We define this as a named Bean so the outer class can inject it
		@Bean("mqttOutboundHandler")
		public MessageHandler mqttOutboundHandler(MqttPahoClientFactory clientFactory) {
			MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(clientId + "-sender", clientFactory);
			messageHandler.setAsync(true);
			messageHandler.setDefaultTopic(responseTopic);
			return messageHandler;
		}
	}
}
