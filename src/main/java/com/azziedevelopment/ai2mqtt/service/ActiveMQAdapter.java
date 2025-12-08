package com.azziedevelopment.ai2mqtt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.azziedevelopment.ai2mqtt.dto.AIRequest;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.type", havingValue = "activemq")
public class ActiveMQAdapter implements MessagingService {

	private final JmsTemplate jmsTemplate;
	private final OpenAIService aiService;
	private final ObjectMapper objectMapper;

	@JmsListener(destination = "ai.prompts")
	public void onMessage(Message message) {
		try {
			if (message instanceof TextMessage textMessage) {
				String payload = textMessage.getText();
				String correlationId = message.getJMSCorrelationID();

				if (correlationId == null) {
					correlationId = message.getJMSMessageID();
				}

				log.debug("Received ActiveMQ Message ID: {}", correlationId);

				AIRequest request;
				try {
					request = objectMapper.readValue(payload, AIRequest.class);

					// Reconstruct if ID is missing in JSON (DTO has 6 fields now)
					if (request.id() == null) {
						request = new AIRequest(
								correlationId,
								request.threadId(),
								request.text(),
								request.systemPrompt(),
								request.maxTokens(),
								request.temperature()
						);
					}
				} catch (Exception e) {
					// Fallback: Payload is raw text
					// We must pass 6 arguments (null for the new fields)
					request = new AIRequest(correlationId, null, payload, null, null, null);
				}

				// Pass systemPrompt (arg 4) to the service
				aiService.processPrompt(
						request.id(),
						request.threadId(),
						request.text(),
						request.systemPrompt()
				);
			}
		} catch (Exception e) {
			log.error("Error processing ActiveMQ message", e);
		}
	}

	@Override
	public void sendResponse(String correlationId, String threadId, String responseText) {
		jmsTemplate.send("ai.responses", session -> {
			TextMessage message = session.createTextMessage(responseText);
			message.setJMSCorrelationID(correlationId);
			message.setStringProperty("threadId", threadId);
			return message;
		});
		log.info("Sent ActiveMQ Response [ID: {}]", correlationId);
	}
}