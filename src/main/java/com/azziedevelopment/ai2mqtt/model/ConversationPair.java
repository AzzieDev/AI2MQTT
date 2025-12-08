package com.azziedevelopment.ai2mqtt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationPair {

	@Id
	@Column(length = 36)
	private String id; // The Message ID (UUID)

	@Column(length = 36, nullable = false)
	private String threadId; // The Conversation Context ID (for chaining)

	@Lob // Allows storing large text blocks
	@Column(columnDefinition = "CLOB")
	private String prompt;

	@Lob
	@Column(columnDefinition = "CLOB")
	private String response;

	private String status; // PENDING, COMPLETED, FAILED

	private LocalDateTime timestamp;

	// Limits used for this specific run (for auditing/debugging)
	private Integer maxTokens;
	private Double temperature;
}
