package com.example.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.example.controllers.ChatSocketTextHandler;
import com.example.controllers.DashboardSocketTextHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
	@Autowired
	ChatSocketTextHandler clienteChat;

	@Autowired
	DashboardSocketTextHandler clienteDashboard;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(clienteChat, "/chat");
		registry.addHandler(clienteDashboard, "/dashboard");
	}
}
