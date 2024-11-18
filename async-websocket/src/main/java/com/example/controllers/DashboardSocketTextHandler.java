package com.example.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.services.DashboardSocketService;

@Component
public class DashboardSocketTextHandler extends TextWebSocketHandler {
	@Autowired
	DashboardSocketService clientes;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		clientes.add(session);
		super.afterConnectionEstablished(session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		clientes.remove(session);
		super.afterConnectionClosed(session, status);
	}

	private Random rnd = new Random();

	static record Punto(int x, int y) {}
	
	@Scheduled(fixedRate = 5000)
	private void generaMetricas() throws IOException {
		if(!clientes.hasSessions()) return;
		Map<String, Object> metricas = new HashMap<>();
		if(rnd.nextBoolean()) {
			var sensores = new ArrayList<Integer>();
			for(var i=1; i++ <= 6; sensores.add(rnd.nextInt(101)));
			metricas.put("sensores", sensores);
		}
		if(rnd.nextBoolean()) {
			var puntos = new ArrayList<Punto>();
			var limite = rnd.nextInt(5) + 5;
			for(var i=1; i++ <= limite; puntos.add(new Punto(rnd.nextInt(120)+40, rnd.nextInt(10)+6)));
			metricas.put("puntos", puntos);
		}
		if(metricas.size() > 0)
			clientes.broadcast(metricas);
	}
	
}