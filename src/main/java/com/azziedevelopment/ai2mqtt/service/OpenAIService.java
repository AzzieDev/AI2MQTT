package com.azziedevelopment.ai2mqtt.service;

import com.azziedevelopment.ai2mqtt.model.ConversationPair;
import com.azziedevelopment.ai2mqtt.model.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpenAIService {

	private final RestClient restClient;
	private final ConversationRepository repository;
	private final MessagingService messagingService;

	private final String defaultModel;
	private final int defaultMaxTokens;
	private final String defaultSystemPrompt;
	private final double defaultTemperature;

	public OpenAIService(ConversationRepository repository,
	                     @Lazy MessagingService messagingService,
	                     RestClient.Builder builder,
	                     @Value("${openai.base-url}") String baseUrl,
	                     @Value("${openai.api.key}") String rawApiKey,
	                     @Value("${openai.model}") String defaultModel,
	                     @Value("${openai.default.max-tokens:500}") int defaultMaxTokens,
	                     @Value("${openai.system-prompt:You are a helpful assistant.}") String defaultSystemPrompt,
	                     @Value("${openai.default.temperature:0.7}") double defaultTemperature) {

		this.repository = repository;
		this.messagingService = messagingService;
		this.defaultModel = defaultModel;
		this.defaultMaxTokens = defaultMaxTokens;
		this.defaultSystemPrompt = defaultSystemPrompt;
		this.defaultTemperature = defaultTemperature;

		// Validation: Catch missing secrets gracefully
		String cleanKey;
		if (rawApiKey == null || rawApiKey.isBlank() || "${GEMINI_API_KEY}".equals(rawApiKey)) {
			log.error("CRITICAL: OpenAI API Key is missing! App will start, but AI features will fail.");
			log.info("Solution: Create 'src/main/resources/secrets.properties' with GEMINI_API_KEY=...");
			cleanKey = "missing-key";
		} else {
			cleanKey = rawApiKey.trim();
			log.info("OpenAI Service Initialized (Key: {}...)", cleanKey.substring(0, Math.min(4, cleanKey.length())));
		}

		this.restClient = builder
			.baseUrl(baseUrl)
			.defaultHeader("Authorization", "Bearer " + cleanKey)
			.defaultHeader("Content-Type", "application/json")
			.build();
	}

	public void processPrompt(String correlationId, String threadId, String promptText, String systemPromptOverride) {
		if (correlationId == null) correlationId = UUID.randomUUID().toString();
		if (threadId == null) threadId = UUID.randomUUID().toString();

		log.info("Processing Prompt [Thread: {}]: {}", threadId, promptText);

		// 1. Fetch History
		List<ConversationPair> history = repository.findByThreadIdOrderByTimestampAsc(threadId);
		List<Map<String, String>> messages = new ArrayList<>();

		// 2. Determine System Prompt
		String effectiveSystemPrompt = (systemPromptOverride != null && !systemPromptOverride.isBlank())
			? systemPromptOverride
			: defaultSystemPrompt;

		messages.add(Map.of("role", "system", "content", effectiveSystemPrompt));

		// 3. Add History
		messages.addAll(history.stream()
			.map(msg -> List.of(
				Map.of("role", "user", "content", msg.getPrompt()),
				Map.of("role", "assistant", "content", (msg.getResponse() != null ? msg.getResponse() : ""))
			))
			.flatMap(List::stream)
			.collect(Collectors.toList()));

		// 4. Add Current User Prompt
		messages.add(Map.of("role", "user", "content", promptText));

		// 5. Save PENDING
		ConversationPair conversation = ConversationPair.builder()
			.id(correlationId)
			.threadId(threadId)
			.prompt(promptText)
			.status("PENDING")
			.timestamp(LocalDateTime.now())
			.build();
		repository.save(conversation);

		try {
			// 6. Call AI Endpoint
			String aiResponse = callAIEndpoint(messages);

			// 7. Update DB & Send Response
			conversation.setResponse(aiResponse);
			conversation.setStatus("COMPLETED");
			repository.save(conversation);

			messagingService.sendResponse(correlationId, threadId, aiResponse);

		} catch (Exception e) {
			log.error("AI Call Failed", e);
			conversation.setStatus("FAILED");
			conversation.setResponse("Error: " + e.getMessage());
			repository.save(conversation);
		}
	}

	private String callAIEndpoint(List<Map<String, String>> messages) {
		// FIX: Include temperature in the request payload
		AIRequestPayload request = new AIRequestPayload(defaultModel, messages, defaultMaxTokens, defaultTemperature);

		return restClient.post()
			.uri("/chat/completions")
			.body(request)
			.retrieve()
			.body(AIResponsePayload.class)
			.choices().get(0).message().content();
	}

	// --- Inner Records (DTOs) for OpenAI JSON ---
	// FIX: Added 'temperature' field to the record
	private record AIRequestPayload(String model, List<Map<String, String>> messages, int max_tokens,
	                                double temperature) {
	}

	private record AIResponsePayload(List<AIChoice> choices) {
	}

	private record AIChoice(AIMessage message) {
	}

	private record AIMessage(String content) {
	}
}
