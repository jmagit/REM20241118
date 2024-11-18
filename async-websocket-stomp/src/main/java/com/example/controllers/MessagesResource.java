package com.example.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.models.MessageDto;

@RestController
@RequestMapping("/api")
public class MessagesResource {
    @Autowired
    private SimpMessagingTemplate template;

	@PostMapping("/send")
	public String requestMethodName(@RequestBody MessageDto message) {
        template.convertAndSend("/topic/messages", message);
		return "SEND TO: " + message.getName() + " MESSAGE: " + message.getText();
	}

}
