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

}
