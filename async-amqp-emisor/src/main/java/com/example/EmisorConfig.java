package com.example;

import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmisorConfig {
    
	@Bean
	AsyncRabbitTemplate asyncRabbitTemplate(RabbitTemplate template) {
		return new AsyncRabbitTemplate(template);
	}
}
