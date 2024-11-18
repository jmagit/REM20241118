package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@EnableScheduling
public class AsyncKafkaSensorApplication implements CommandLineRunner {
    private static final Logger LOG = Logger.getLogger(AsyncKafkaSensorApplication.class.getName());

	public static void main(String[] args) {
		SpringApplication.run(AsyncKafkaSensorApplication.class, args);
	}
	
	@Value("${app.topic.name}") 
	private String tema;
	@Value("${app.sensor.id}") 
	private String idSensor;
	@Value("${app.dorsales}") 
	private int dorsales;

	private Random rnd = new Random();
	@Override
	public void run(String... args) throws Exception {
		sendEvent(tema + "-logger", idSensor, String.format("INFO - Arranca el sensor: %s", idSensor));
		var peloton = new ArrayList<Integer>();
		for(var i=1; i <= dorsales; peloton.add(i++));
		Collections.shuffle(peloton);
		
		for(var dorsal: peloton) {
			sendEvent(tema, idSensor, dorsal.toString());
			Thread.sleep(rnd.nextInt(5) * 500);
		}
		sendEvent(tema + "-logger", idSensor, String.format("INFO - Termina el sensor: %s", idSensor));
		System.exit(0);
	}
	
	record Evento(String sensor, @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss") Date enviado) {}
	ObjectMapper converter = new ObjectMapper();
	
	@Scheduled(fixedDelay = 1000)
	private void telemetria() throws JsonProcessingException {
		sendEvent(tema + "-control", idSensor, converter.writeValueAsString(new Evento(idSensor, new Date())));
	}
	
	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;
	
	private void sendEvent(String topic, String origen, String value) {
		kafkaTemplate.send(topic, origen, value)
			.thenAccept(result -> LOG.info(String.format("TOPIC: %s, KEY: %s, VALUE: %s, OFFSET: %s", 
					topic, origen, value, result.getRecordMetadata().offset())))
			.exceptionally(ex -> {
				LOG.severe(String.format("TOPIC: %s, KEY: %s, VALUE: %s, ERROR: %s", 
					topic, origen, value, ex.getMessage()));
				sendEvent(tema + "-logger", idSensor, String.format("ERROR - TOPIC: %s, KEY: %s, VALUE: %s, ERROR: %s", 
						topic, origen, value, ex.getMessage()));
				return null;
			});
	}

}
