package com.azziedevelopment.ai2mqtt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * General application infrastructure configuration.
 * Defines global beans used across the application.
 */
@Configuration
public class AppConfig {

	@Bean
	public RestClient.Builder restClientBuilder() {
		// FIX: Force HTTP/1.1 to prevent vLLM/LocalAI connection drops
		// Many local python servers don't handle Java's HTTP/2 upgrade well.
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
			HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1) // Force HTTP/1.1
				.connectTimeout(Duration.ofSeconds(10))
				.build()
		);
		requestFactory.setReadTimeout(Duration.ofMinutes(5)); // Long timeout for AI generation

		return RestClient.builder()
			.requestFactory(requestFactory);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
