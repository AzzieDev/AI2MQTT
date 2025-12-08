package com.azziedevelopment.ai2mqtt;

import com.fasterxml.jackson.databind.ObjectMapper; // Import added
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

@Slf4j
@SpringBootApplication
@EnableAsync
public class AI2MQTTApplication {

	static void main(String[] args) {
		SpringApplication.run(AI2MQTTApplication.class, args);
	}

	@Bean
	public RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public CommandLineRunner startupLogger(@Value("${messaging.type}") String messagingType,
	                                       @Value("${openai.base-url}") String aiBaseUrl) {
		return args -> {
			log.info("============================================================");
			log.info("   AI2MQTT STARTED SUCCESSFULLY");
			log.info("============================================================");
			log.info("   Messaging Mode: {}", messagingType.toUpperCase());
			log.info("   AI Provider:    {}", determineProviderName(aiBaseUrl));
			log.info("   AI Base URL:    {}", aiBaseUrl);
			log.info("   Dashboard:      http://localhost:8080");
			log.info("============================================================");
		};
	}

	private String determineProviderName(String url) {
		if (url.contains("api.openai.com")) return "OpenAI (Cloud)";
		if (url.contains("localhost") || url.contains("127.0.0.1")) return "Local/vLLM (Private)";
		return "Custom Endpoint";
	}
}