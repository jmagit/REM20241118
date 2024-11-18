package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

@SpringBootApplication
public class AsyncKafkaConsumerApplication {
    private static final Logger LOG = Logger.getLogger(AsyncKafkaConsumerApplication.class.getName());
	@Value("${app.topic.name}") 
	private String tema;

	public static void main(String[] args) {
		SpringApplication.run(AsyncKafkaConsumerApplication.class, args);
	}

	@Profile("list")
	@Configuration
	private static class ListConfig {
		private static class Listener {
			@KafkaListener(topics = "${app.topic.name}", topicPattern = "${app.topic.name}")
			public void listenWithHeaders(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload String value,
					@Header(KafkaHeaders.OFFSET) String offset) {
				System.out.println(String.format("KEY: %s, MESSAGE: %s, OFFSET: %s", key, value, offset));
			}
		}
		@Bean
		Listener runner() {
			return new Listener();
		}
	}

	@Profile("calc")
	@Configuration
	private static class CalcConfig {
		private static class Listener {
			Map<String, Integer> contadores = new HashMap<>();
			
			@KafkaListener(topics = "${app.topic.name}", topicPattern = "${app.topic.name}")
			public void listenWithHeaders(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload String value,
					@Header(KafkaHeaders.OFFSET) String offset) {
				int count = contadores.containsKey(key) ? contadores.get(key) + 1 : 1;
				contadores.put(key, count);
				System.out.println("Nuevo Resumen");
				contadores.forEach((clave, valor) -> {
					System.out.println(String.format("KEY: %s, COUNT: %s", clave, valor));
				});
			}
		}
		@Bean
		Listener runner() {
			return new Listener();
		}
	}
}
