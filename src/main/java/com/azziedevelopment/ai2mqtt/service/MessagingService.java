package com.azziedevelopment.ai2mqtt.service;

public interface MessagingService {

	/**
	 * Sends a processed AI response back to the messaging grid.
	 * * @param correlationId The Unique ID of the original request (UUID)
	 * @param threadId      The Conversation ID (for history context)
	 * @param responseText  The final answer from the AI
	 */
	void sendResponse(String correlationId, String threadId, String responseText);
}