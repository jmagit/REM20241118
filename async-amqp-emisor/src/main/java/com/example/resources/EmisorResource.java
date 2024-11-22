package com.example.resources;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Logger;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.RabbitConverterFuture;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.models.MessageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
public class EmisorResource {
	private static final Logger LOGGER = Logger.getLogger(EmisorResource.class.getName());

	@Value("${spring.application.name}:${server.port}")
	private String origen;

	@Autowired
	private Queue saludosQueue;

	@Autowired
	private AmqpTemplate amqp;

	@GetMapping(path = "/saludo/{nombre}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "basic" }, summary = "Generar automáticamente el mensaje")
	public String saluda(@PathVariable String nombre) {
		var msg = "Hola " + nombre;
		amqp.convertAndSend(saludosQueue.getName(), new MessageDTO(msg, origen));
		return "SEND: " + msg;
	}

	/**
	 * Envío masivo de mansajes
	 * @param cantidad
	 * @return
	 * @throws InterruptedException
	 */
	@GetMapping(path = "/saludos/{cantidad}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "basic" })
	public String saludos(@PathVariable int cantidad) throws InterruptedException {
		for (int i = 1; i <= cantidad; i++) {
			amqp.convertAndSend(saludosQueue.getName(), new MessageDTO("Saludo nº: " + i, origen));
			Thread.sleep(500);
		}
		return "Enviados: " + cantidad;
	}

	@Autowired
	Queue deadLetterQueue;

	@GetMapping(path = "/fallido")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "dead-letter" }, summary = "Envía un mensaje fallido que debe ser rechazado y acabar en una cola dead letter")
	public String fallido() {
		amqp.convertAndSend(saludosQueue.getName(), new MessageDTO(null, origen));
		return "Enviando un mensaje defectuoso";
	}

	@GetMapping(path = "/message/{mensaje}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "dead-letter" }, summary = "Crear manualmente el mensaje")
	public String raw(@PathVariable String mensaje) throws JsonProcessingException {
		var payload = new MessageDTO(mensaje, origen);
		ObjectMapper mapper = new ObjectMapper();
		var message = MessageBuilder.withBody(mapper.writeValueAsString(payload).getBytes()).build();
		amqp.send(deadLetterQueue.getName(), message);
		return "SEND: " + mapper.writeValueAsString(message);
	}

	@GetMapping(path = "/nexterror")
	@Operation(tags = { "dead-letter" }, summary = "Recupera manualmente el siguiente mensaje fallido de la cola dead letter")
	public ResponseEntity<?> nextError() {
		var last = amqp.receiveAndConvert(deadLetterQueue.getName());
		if(last == null)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok(last);
	}
	
	@Value("${app.topic.exchange}") 
	private String topicExchange;
	
	@GetMapping(path = "/topic/{message}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "routing" }, summary = "Envia un mensaje a un tema")
	public String topic(@PathVariable String message, 
			@RequestParam @Parameter(schema = @Schema(type = "string", allowableValues = {"error", "aviso", "comunicado", "local" }, defaultValue = "comunicado")) String tipo, 
			@RequestParam @Parameter(schema = @Schema(type = "string", allowableValues = {"urgente", "importante", "normal" }, defaultValue = "normal")) String prioridad
			) {
		String routingKey = tipo + "." + prioridad;
		amqp.convertAndSend(topicExchange, routingKey, new MessageDTO(message, origen), 
				m -> { LOGGER.warning("ENVIADO: " + m.toString()); return m; });
		return "TOPIC: " + routingKey + " SEND: " + message;
	}

	@Value("${app.idiomas.exchange}") 
	private String topicHeaders;
	
	@GetMapping(path = "/headers/{nombre}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "routing" }, summary = "Enrutar un mensaje usando la cabecera")
	public String headers(@PathVariable String nombre, 
			@RequestParam @Parameter(schema = @Schema(type = "string", allowableValues = {"es", "ca", "eu", "gl" }, defaultValue = "es")) String lang
			) throws JsonProcessingException {
		var message = switch (lang) { case "eu" -> "Kaixo"; case "gl" -> "Ola"; default -> "Hola"; } + " " + nombre; 
		amqp.convertAndSend(topicHeaders, "", new MessageDTO(message, origen), 
				m -> {  m.getMessageProperties().setHeader("lang", lang);  return m; });
		return "SEND: " + message;
//		var payload = new MessageDTO(switch (lang) { case "eu" -> "Kaixo"; case "gl" -> "Ola"; default -> "Hola"; } + " " + nombre, origen);
//		ObjectMapper mapper = new ObjectMapper();
//		var message = MessageBuilder.withBody(mapper.writeValueAsString(payload).getBytes())
//				.setContentType(MessageProperties.CONTENT_TYPE_JSON)
//				.setContentEncoding("UTF-8")
//				.setHeader("lang", lang)
//				.build();
//		amqp.send(topicHeaders, "", message);
//		return "SEND: " + mapper.writeValueAsString(message);
	}

	@Value("${app.rpc.routing-key}")
	String routingKey;

	@Autowired
	DirectExchange rpcExchange;

	@Autowired
	AsyncRabbitTemplate asyncRabbitTemplate;

	private Map<UUID, MessageDTO> respuestas = new HashMap<>();
	private UUID lastRequest;
	
	private void procesaRespuesta(Object response) {
		LOGGER.warning("Respuesta recibida: " + response);
		if (response instanceof MessageDTO m) {
			respuestas.put(m.getId(), m);
		} else {
			LOGGER.severe("Formato de respuesta desconocido");
		}
	}

	public static record RequestStatus(String statusQuery, String pause, String resume, String terminate, String cancel) {}
	
	@PostMapping(path = "/x-rpc/peticion/{nombre}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "web-hooks" }, summary = "Realiza una petición asíncrona", 
			description = "Obtiene las url para hacer el seguimiento de la petición. "
					+ "Para establecer el **tiempo de respuesta**, el receptor tarda 0,5s por letra del nombre.")
	public RequestStatus peticion(@PathVariable String nombre) {
		var peticion = new MessageDTO("Petición de " + nombre, origen);
		lastRequest = peticion.getId();
		
		asyncRabbitTemplate.convertSendAndReceive(rpcExchange.getName(), routingKey, peticion)
				.thenAccept(result -> procesaRespuesta(result))
				.exceptionally(ex -> {
					LOGGER.severe(ex.toString());
					return null;
				});
		var url = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(peticion.getId()).toUri().toString();
		return new RequestStatus(
				url.replace("/peticion/" + nombre, "/seguimiento"),
				url.replace("/peticion/" + nombre, "/pausa"),
				url.replace("/peticion/" + nombre, "/reanudacion"),
				url.replace("/peticion/" + nombre, "/finalizacion"),
				url.replace("/peticion/" + nombre, "/cancelacion")
				);
	}

	@GetMapping(path = "/x-rpc/seguimiento/{id}")
	@Operation(tags = { "web-hooks" }, summary = "Consulta el estado de una petición asíncrona por su identificador", 
		description = "El cliente sondea la dirección URL en el encabezado de ubicación y continúa viendo las respuestas HTTP 202 mientras se encruentre pendiente.\r\n"
				+ "Cuando la petición finaliza o se produce un error, recibe un HTTP 200 y el resultado de la petición.")
	public ResponseEntity<?> seguimiento(@PathVariable UUID id) {
		if(respuestas.containsKey(id)) {
			return ResponseEntity.ok(respuestas.get(id));
		} else {
			return ResponseEntity.accepted().build();
		}
	}

	@GetMapping(path = "/x-rpc/seguimiento")
	@Operation(tags = { "web-hooks" }, summary = "Atajo para consultar el estado de la última petición")
	public ResponseEntity<?> seguimientoTest() {
		return seguimiento(lastRequest);
	}

	@GetMapping(path = "/x-rpc/respuestas")
	@Operation(tags = { "web-hooks" }, summary = "Lista los resultados de las peticiones asíncronas finalizadas")
	public Collection<MessageDTO> todasLasRespuestas() {
		return respuestas.values().stream().sorted((a, b) -> b.getEnviado().compareTo(a.getEnviado())).toList();
	}

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@GetMapping(path = "/sinconfirm/{mensaje}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "publisher-confirms" }, summary = "Envia el mensaje sin confirmación")
	public String sinConfirm(@PathVariable String mensaje, @RequestParam String exchange) {
		amqp.convertAndSend(exchange, "", mensaje);
		return "SEND: " + mensaje; 
	}
	
	@GetMapping(path = "/confirm/{mensaje}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "publisher-confirms" }, summary = "Envia el mensaje con confirmación")
	public String confirm(@PathVariable String mensaje, @RequestParam String exchange) {
		rabbitTemplate.setConfirmCallback((correlation, ack, reason) -> {
			if (correlation != null) {
				LOGGER.info("Received " + (ack ? "ack" : "nack") + " for correlation: " + correlation);
			} else {
				LOGGER.severe("No correlation");
			}
		});
		rabbitTemplate.setReturnsCallback(returned -> {
			LOGGER.info("Returned: " + returned.getMessage() + "\nreplyCode: " + returned.getReplyCode()
					+ "\nreplyText: " + returned.getReplyText() + "\nexchange/rk: "
					+ returned.getExchange() + "/" + returned.getRoutingKey());
		});
		CorrelationData correlationData = new CorrelationData("Correlation for " + UUID.randomUUID().toString());
		rabbitTemplate.convertAndSend(exchange, "", mensaje, correlationData);
		Confirm confirm;
		try {
			confirm = correlationData.getFuture().get(10, TimeUnit.SECONDS);
			return confirm.isAck() ? ("SEND: " + mensaje) : ("NACK: " + confirm.getReason());
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			return e.getMessage();
		}
	}
	
	

	@GetMapping(path = "/coreografia/{procesoId}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "workflow" }, summary = "Lanza una coreografia")
	public String lanza(@PathVariable("procesoId") int id) {
		var m = new MessageDTO("Proceso " + id + " (" + origen + ")", origen);
		amqp.convertAndSend("demos.coreografia.paso1", m);
		return "Inicio proceso " + id; 
	}

	@GetMapping(path = "/orquestacion/{procesoId}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(tags = { "workflow" }, summary = "Lanza una orquestacion")
	public String orquesta(@PathVariable("procesoId") int id) {
		var peticion = new MessageDTO("Proceso " + id + " (" + origen + ")", origen);
		
		LOGGER.info("Arranca la orquestación");
		var orquestacion = new ArrayDeque<String>();
		orquestacion.add("demos.orquetacion.pasoA");
		orquestacion.add("demos.orquetacion.pasoB");
		orquestacion.add("demos.orquetacion.pasoC");
//		orquestacion.add("");
		runOrquestacion(orquestacion, peticion);
//		asyncRabbitTemplate.convertSendAndReceive("demos.orquetacion.pasoA", peticion)
//				.thenAccept(result1 -> {	
//					LOGGER.info("PASO 1: " + result1.toString());
//					asyncRabbitTemplate.convertSendAndReceive("demos.orquetacion.pasoB", result1)
//						.thenAccept(result2 -> {
//							LOGGER.info("PASO 2: " + result2.toString());
//							})
//						.exceptionally(onOrquestationException());})
//				.exceptionally(onOrquestationException());
		return "Inicio proceso " + id; 
	}

    private CompletableFuture<Void> runOrquestacion(java.util.Queue<String> steps, Object message) {
    	String routingKey = steps.poll();
		return asyncRabbitTemplate.convertSendAndReceive(routingKey, message)
			.thenAccept(result -> {
					LOGGER.info(routingKey + ": " + result.toString());
					if(steps.isEmpty()) {
						LOGGER.info("Fin de la orquestación");
					} else {
						runOrquestacion(steps, result);
					}
				})
			.exceptionally(onOrquestationException());
    }

	private Function<Throwable, ? extends Void> onOrquestationException() {
		return ex -> {
			LOGGER.info(ex.toString());
			return null;
		};
	}

}
