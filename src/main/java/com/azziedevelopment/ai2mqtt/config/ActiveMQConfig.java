package com.azziedevelopment.ai2mqtt.config;

import jakarta.jms.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * Ensures the necessary infrastructure beans for ActiveMQ are present
 * when ActiveMQ is the selected messaging provider.
 */
@Configuration
@ConditionalOnProperty(name = "messaging.type", havingValue = "activemq")
public class ActiveMQConfig {

    /**
     * Defines the required JmsListenerContainerFactory for the @JmsListener annotation.
     */
    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        
        // This is important for processing topic messages concurrently
        factory.setPubSubDomain(true); 
        
        // Optional: Ensure messages are processed in parallel (better throughput)
        factory.setConcurrency("3-10"); 
        
        return factory;
    }
}
