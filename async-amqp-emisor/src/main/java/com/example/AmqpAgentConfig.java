package com.example;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpAgentConfig {
	@Bean
	MessageConverter jsonConverter() {
		return new Jackson2JsonMessageConverter();
	}

//	@Bean
//	Queue saludosQueue(@Value("${app.cola}") String queue) {
//        return new Queue(queue);
//	}

	@Bean
	Queue saludosQueue(@Value("${app.cola}") String queue, FanoutExchange deadLetterExchange, Queue deadLetterQueue) {
		return QueueBuilder.durable(queue)
				.deadLetterExchange(deadLetterExchange.getName())
				.deadLetterRoutingKey(deadLetterQueue.getName())
				.build();
	}

	@Bean
	Queue deadLetterQueue(@Value("${app.cola}.dlq") String queue) {
		return new Queue(queue);
	}

	@Bean
	FanoutExchange deadLetterExchange(@Value("${app.cola}.dlx") String exchange) {
		return new FanoutExchange(exchange);
	}

	@Bean
	Binding deadLetterBinding(FanoutExchange deadLetterExchange, Queue deadLetterQueue) {
		return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange);
	}

	@Bean
	Queue rpcQueue(@Value("${app.rpc.queue}") String queue) {
		return new Queue(queue);
	}

	@Bean
	DirectExchange rpcExchange(@Value("${app.rpc.exchange}") String exchange) {
		return new DirectExchange(exchange);
	}

	@Bean
	Binding rpcBinding(DirectExchange rpcExchange, Queue rpcQueue, @Value("${app.rpc.routing-key}") String routingKey) {
		return BindingBuilder.bind(rpcQueue).to(rpcExchange).with(routingKey);
	}
}
