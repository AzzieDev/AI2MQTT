package com.azziedevelopment.ai2mqtt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.type", havingValue = "mqtt")
public class HAAutoDiscovery {

	@Qualifier("mqttOutboundHandler")
	private final MessageHandler mqttOutboundHandler;

	@Value("${mqtt.topic.responses}")
	private String responseTopic;

	@EventListener(ApplicationReadyEvent.class)
	public void announceDevice() {
		log.info("Announcing AI2MQTT Device to Home Assistant...");

		// UPDATE:
		// 1. We truncate the main state to 250 chars to avoid "Unknown" errors.
		// 2. We put the FULL response into a 'full_text' attribute.
		String configPayload = """
            {
              "name": "AI Last Response",
              "unique_id": "ai2mqtt_last_response",
              "state_topic": "%s",
              "value_template": "{{ value_json.response | truncate(250) }}",
              "json_attributes_topic": "%s",
              "json_attributes_template": "{{ {'full_text': value_json.response} | tojson }}",
              "icon": "mdi:robot",
              "device": {
                "identifiers": ["ai2mqtt_bridge"],
                "name": "AI2MQTT Bridge",
                "model": "Spring Boot Service",
                "manufacturer": "Azzie Development",
                "sw_version": "1.0.0"
              }
            }
            """.formatted(responseTopic, responseTopic);

		String discoveryTopic = "homeassistant/sensor/ai2mqtt/response/config";

		Message<String> message = MessageBuilder
				.withPayload(configPayload)
				.setHeader("mqtt_topic", discoveryTopic)
				.setHeader("mqtt_retained", true)
				.build();

		try {
			mqttOutboundHandler.handleMessage(message);
			log.info("Discovery packet sent to: {}", discoveryTopic);
		} catch (Exception e) {
			log.error("Failed to send discovery packet", e);
		}
	}
}