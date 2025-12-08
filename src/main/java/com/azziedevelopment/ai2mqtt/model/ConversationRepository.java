package com.azziedevelopment.ai2mqtt.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationPair, String> {

	/**
	 * Finds all messages in a specific thread, ordered by oldest first.
	 * This is crucial for constructing the "Context Window" for the AI.
	 */
	List<ConversationPair> findByThreadIdOrderByTimestampAsc(String threadId);

	// Used by the Dashboard to show newest chats first
	List<ConversationPair> findAllByOrderByTimestampDesc();
}