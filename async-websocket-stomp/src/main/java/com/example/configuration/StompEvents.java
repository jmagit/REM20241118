package com.example.configuration;

import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.service.StompService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class StompEvents {
  private final StompService socketService;

  @EventListener
  public void onDisconnectEvent(SessionDisconnectEvent event) {
    log.debug("Client with session id {} disconnected", event.getSessionId());
    String sessionId = event.getSessionId();
    String name = socketService.getNameBySession(sessionId);
    socketService.removeSession(sessionId);
    log.debug("Client with name {} has been disconnected ", name);
  }

}
