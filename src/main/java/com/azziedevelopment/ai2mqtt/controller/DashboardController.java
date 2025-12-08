package com.azziedevelopment.ai2mqtt.controller;

import com.azziedevelopment.ai2mqtt.model.ConversationPair;
import com.azziedevelopment.ai2mqtt.model.ConversationRepository;
import com.azziedevelopment.ai2mqtt.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

	private final ConversationRepository repository;
	private final OpenAIService openAIService;

	@GetMapping("/")
	public String dashboard(Model model) {
		List<ConversationPair> allMessages = repository.findAll();

		Map<String, List<ConversationPair>> groupedChats = allMessages.stream()
			.sorted(Comparator.comparing(ConversationPair::getTimestamp).reversed())
			.collect(Collectors.groupingBy(ConversationPair::getThreadId));

		model.addAttribute("groupedChats", groupedChats);
		return "index";
	}

	// UPDATE: Added 'systemPrompt' parameter
	@PostMapping("/send")
	public String sendPrompt(@RequestParam("prompt") String prompt,
	                         @RequestParam(value = "threadId", required = false) String threadId,
	                         @RequestParam(value = "systemPrompt", required = false) String systemPrompt) {

		String effectiveThreadId = (threadId == null || threadId.isBlank())
			? UUID.randomUUID().toString()
			: threadId;

		String correlationId = UUID.randomUUID().toString();

		// Pass the systemPrompt (can be null/empty, service handles fallback)
		openAIService.processPrompt(correlationId, effectiveThreadId, prompt, systemPrompt);

		return "redirect:/";
	}
}
