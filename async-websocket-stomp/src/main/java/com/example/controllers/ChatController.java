package com.example.controllers;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.models.MessageDto;
import com.example.service.StompService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatController {
	private static final Logger log = LoggerFactory.getLogger(ChatController.class);

	@Value("ws://localhost:${server.port:8080}/chat?STOMP")
	private String WEBSOCKET_SERVER_URL;

	private final StompService socketService;
	private WebSocketSession client;

	@MessageMapping("/chat")
	@SendTo("/topic/messages")
	public MessageDto sendMessage(MessageDto message, @Header("simpSessionId") String sessionId) throws InterruptedException, ExecutionException, IOException {
		if (client == null || !client.isOpen()) {
			WebSocketClient ws = new StandardWebSocketClient();
			client = ws.execute(new TextWebSocketHandler(), WEBSOCKET_SERVER_URL).get();
		}
		if(client.isOpen()) {
			client.sendMessage(new TextMessage("[" + message.getName() + "]: " + message.getText()));
		} else {
			log.error("Sesión cerrada");
		}
		
		socketService.saveSession(sessionId, message.getName());

		return message;
	}

}
