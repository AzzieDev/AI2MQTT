package com.azziedevelopment.ai2mqtt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	// Provide dummy values so @Value fields don't crash the startup
	"openai.api.key=dummy-test-key",
	"mqtt.username=test-user",
	"mqtt.password=test-pass",
	"mqtt.broker.url=tcp://localhost:1883",
	"spring.datasource.url=jdbc:h2:mem:testdb", // Use in-memory DB for tests
	"messaging.type=mqtt" // Force a specific mode to test
})
class AI2MQTTApplicationTests {

	@Test
	void contextLoads() {
	}

}
