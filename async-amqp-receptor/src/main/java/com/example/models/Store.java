package com.example.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Value;

public class Store {
	@Value
	public static class Message {
		private MessageDTO msg;
		private Date recibido = new Date();
		public Message(MessageDTO msg) {
			this.msg = msg;
		}
		
	}
	private static List<Message> msgs = new ArrayList<>();
	
	public static void add(MessageDTO msg) {
		msgs.add(new Message(msg));
	}
	public static List<Message> get() {
		return msgs.stream().toList();
	}
	public static List<Message> getDesc() {
		return msgs.stream().sorted((a, b) -> b.getRecibido().compareTo(a.getRecibido())).toList();
	}
	
}
