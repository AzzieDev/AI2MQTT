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
@ConditionalOnProperty(name = "homeassistant.discovery.enabled", havingValue = "true")
public class HAAutoDiscovery {

	@Qualifier("mqttOutboundHandler")
	private final MessageHandler mqttOutboundHandler;

	@Value("${mqtt.topic.responses}")
	private String responseTopic;

	@Value("${mqtt.topic.prompts}")
	private String promptTopic;

	@EventListener(ApplicationReadyEvent.class)
	public void announceDevice() {
		log.info("Announcing AI2MQTT Device (Sensors) to Home Assistant...");

		// 1. ANNOUNCE RESPONSE SENSOR
		sendDiscoveryPacket(
			"AI Last Response",
			"ai2mqtt_last_response",
			responseTopic,
			"{{ value_json.response | truncate(250) }}",
			"{{ {'full_text': value_json.response, 'thread_id': value_json.threadId} | tojson }}",
			"mdi:robot"
		);

		// 2. ANNOUNCE PROMPT SENSOR (New!)
		sendDiscoveryPacket(
			"AI Last Prompt",
			"ai2mqtt_last_prompt",
			promptTopic,
			"{{ value_json.text | truncate(250) }}",
			"{{ {'full_text': value_json.text, 'thread_id': value_json.threadId, 'model': value_json.model} | tojson }}",
			"mdi:message-text"
		);
	}

	private void sendDiscoveryPacket(String name, String uniqueId, String stateTopic, String valueTemplate, String attrTemplate, String icon) {
		String configPayload = """
			{
			  "name": "%s",
			  "unique_id": "%s",
			  "state_topic": "%s",
			  "value_template": "%s",
			  "json_attributes_topic": "%s",
			  "json_attributes_template": "%s",
			  "icon": "%s",
			  "device": {
			    "identifiers": ["ai2mqtt_bridge"],
			    "name": "AI2MQTT Bridge",
			    "model": "Spring Boot Service",
			    "manufacturer": "Azzie Development",
			    "sw_version": "1.0.0"
			  }
			}
			""".formatted(name, uniqueId, stateTopic, valueTemplate, stateTopic, attrTemplate, icon);

		// Discovery Topic Format: homeassistant/sensor/<node_id>/<object_id>/config
		String discoveryTopic = "homeassistant/sensor/ai2mqtt/" + uniqueId + "/config";

		Message<String> message = MessageBuilder
			.withPayload(configPayload)
			.setHeader("mqtt_topic", discoveryTopic)
			.setHeader("mqtt_retained", true)
			.build();

		try {
			mqttOutboundHandler.handleMessage(message);
			log.info("Discovery packet sent: {}", uniqueId);
		} catch (Exception e) {
			log.error("Failed to send discovery packet for {}", uniqueId, e);
		}
	}
}
