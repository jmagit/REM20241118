package com.example.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.Value;

@Component
public class ChatSocketService {
	public static record Session(String name, WebSocketSession session) {}

	private Map<String, Session> sesiones = new HashMap<>();

	public List<WebSocketSession> getAll() {
		return sesiones.values().stream().map(i -> i.session()).toList();
	}

	public void add(String name, WebSocketSession session) {
		sesiones.put(session.getId(), new Session(name, session));
	}

	public void remove(WebSocketSession session) {
		sesiones.remove(session.getId());
	}

	public String getName(WebSocketSession session) {
		return sesiones.get(session.getId()).name();
	}

	public Optional<WebSocketSession> getSession(String name) {
		var session = sesiones.get(name);
		return session == null ? Optional.empty() : Optional.ofNullable(session.session);
	}


	public void sendMessage(String name, String message) throws IOException {
		var client = getSession(name);
		if(client.isPresent() && client.get().isOpen()) {
			client.get().sendMessage(new TextMessage(message));
		}
	}

	public void broadcast(String message) throws IOException {
		for (WebSocketSession client : getAll()) {
			client.sendMessage(new TextMessage(message));
		}
	}
}
