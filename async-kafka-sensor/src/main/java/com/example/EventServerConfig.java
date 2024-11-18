package com.example;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventServerConfig {

	@Bean
	NewTopic topicLocation(@Value("${app.topic.name}") String tema) {
		return new NewTopic(tema, 4, (short)1);
	}

	@Bean
	NewTopic topicTelemetria(@Value("${app.topic.name}-control") String tema) {
		return new NewTopic(tema, 1, (short)1);
	}

	@Bean
	NewTopic topicLogger(@Value("${app.topic.name}-logger") String tema) {
		return new NewTopic(tema, 1, (short)1);
	}

}
