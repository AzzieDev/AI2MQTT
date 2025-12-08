package com.azziedevelopment.ai2mqtt.dto;

public record AIRequest(
		String id,            // Unique Message ID
		String threadId,      // Conversation Context ID
		String text,          // The Prompt
		String systemPrompt,  // Optional System Prompt Override
		Integer maxTokens,    // Optional override
		Double temperature    // Optional override
) {}