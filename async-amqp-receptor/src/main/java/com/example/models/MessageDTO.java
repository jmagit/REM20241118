package com.example.models;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
public class MessageDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private UUID id = UUID.randomUUID();
	private String msg;
	private String origen;
	@JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss:SSS")
	private Date enviado = new Date();
	
	public MessageDTO(String msg, String origen) {
		this.msg = msg;
		this.origen = origen;
	}
	
	
}
