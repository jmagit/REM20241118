package com.example;

import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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

	@Bean
	HeadersExchange idiomasExchange(@Value("${app.idiomas.exchange}") String name, AmqpAdmin adm) {
		var exchange = new HeadersExchange(name);
		adm.declareExchange(exchange);
		for(var lang: List.of("es","ca","eu","gl")) {
			var queue = new Queue(name + "." + lang);
			adm.declareQueue(queue);
			adm.declareBinding(BindingBuilder.bind(queue).to(exchange).whereAny(Map.of("lang", lang)).match());
		}
		return exchange;
	}

//	
//	 Policy: AE-Temas
//		Pattern	^demos\.temas$
//		Apply to	exchanges
//		Definition	alternate-exchange:	demos.temas.default
//		Priority	0
//	
	@Bean
	TopicExchange topicExchange(@Value("${app.topic.exchange}") String name, HeadersExchange idiomasExchange, AmqpAdmin adm) {
		var temas = Map.of(
				"comunes", List.of("*.normal","comunicado.*"),
				"urgentes", List.of("*.urgente","error.#")
				);
		var exchange = new TopicExchange(name);
		var exchangeDefault = new TopicExchange(name + ".default");
		adm.declareExchange(exchange);
		adm.declareExchange(exchangeDefault);
		for(var tema : temas.keySet() ) {
			var queue = new Queue(name + "." + tema);
			adm.declareQueue(queue);
			for(var routingKey : temas.get(tema) ) {
				adm.declareBinding(BindingBuilder.bind(queue).to(exchange).with(routingKey));
			}
			if("comunes".equals(tema))
				adm.declareBinding(BindingBuilder.bind(queue).to(exchangeDefault).with("#"));
		}
		adm.declareBinding(BindingBuilder.bind(idiomasExchange).to(exchange).with("local.*"));
		return exchange;
	}

	@Bean
	DirectExchange orquetacionExchange(@Value("${app.orquetacion.exchange}") String name, AmqpAdmin adm) {
		var exchange = new DirectExchange(name);
		adm.declareExchange(exchange);
		for(var paso: List.of("A","B","C")) {
			var queue = new Queue(name + ".paso" + paso);
			adm.declareQueue(queue);
			adm.declareBinding(BindingBuilder.bind(queue).to(exchange).with("paso" + paso));
		}
		return exchange;
	}

	@Bean
	DirectExchange coreografiaExchange(@Value("${app.coreografia.exchange}") String name, AmqpAdmin adm) {
		var exchange = new DirectExchange(name);
		adm.declareExchange(exchange);
		for(var paso: List.of("1","2","3","4")) {
			var queue = new Queue(name + ".paso" + paso);
			adm.declareQueue(queue);
			adm.declareBinding(BindingBuilder.bind(queue).to(exchange).with("paso" + paso));
		}
		return exchange;
	}

}
