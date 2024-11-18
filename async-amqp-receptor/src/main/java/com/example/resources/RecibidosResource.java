package com.example.resources;

import java.util.List;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.ReceptorConfig;
import com.example.models.MessageDTO;
import com.example.models.Store;
import com.example.models.Store.Message;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class RecibidosResource {
	@Value("${spring.application.name}:${server.port}")
	private String origen;
	
	@Autowired
	private Queue saludosQueue;
	
	@Autowired
	private AmqpTemplate amqp;
	
	@GetMapping(path = "/recibidos")
	@Operation(summary = "Lista los mensajes recibidos en orden descendente.")
	public List<Message> recibidos() {
		return Store.getDesc();
	}
	
	@GetMapping(path = "/estado")
	@Operation(summary = "Cambia el receptor de pausado a reanudado y viceversa, informando del nuevo estado", 
		description = "En estado pausado, los mensajes recibidos generan una excepción para que se reintegren a la cola.")
	public String cambia() {
		return "El servicio se ha " + (ReceptorConfig.cambia() ? "pausado." : "reanudado.");
	}
}
