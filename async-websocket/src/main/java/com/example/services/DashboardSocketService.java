package com.example.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DashboardSocketService {
	private List<WebSocketSession> sesiones = new ArrayList<>();

	public List<WebSocketSession> getAll() {
		return sesiones;
	}

	public boolean hasSessions() {
		return sesiones.size() > 0;
	}
	public void add(WebSocketSession session) {
		sesiones.add(session);
	}

	public void remove(WebSocketSession session) {
		sesiones.remove(session);
	}

	private ObjectMapper serializador = new ObjectMapper();
	
	public void broadcast(Map<String, Object> metricas) throws IOException {
		for (WebSocketSession client : sesiones) {
			client.sendMessage(new TextMessage(serializador.writeValueAsString(metricas)));
		}
	}
}
