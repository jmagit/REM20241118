package com.example.controllers;

import java.io.IOException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.services.ChatSocketService;

@Component
public class ChatSocketTextHandler extends TextWebSocketHandler {
	@Autowired
	ChatSocketService clientes;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		if (session.getUri().getQuery() != null) {
			clientes.add(session.getUri().getQuery(), session);
			session.sendMessage(new TextMessage("Servidor: Te has conectado como " + session.getUri().getQuery()));
		} else
			session.close(new CloseStatus(1006, "Falta el nombre de usuario"));
		super.afterConnectionEstablished(session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		clientes.remove(session);
		super.afterConnectionClosed(session, status);
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws InterruptedException, IOException {
		String senderName = clientes.getName(session);
		String payload = message.getPayload();
		clientes.broadcast(senderName + ": " + payload);
	}

}