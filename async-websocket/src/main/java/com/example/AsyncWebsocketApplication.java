package com.example;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.example.models.MessageDto;
import com.example.services.ChatSocketService;

@SpringBootApplication
@EnableScheduling
public class AsyncWebsocketApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(AsyncWebsocketApplication.class, args);
	}

	@Autowired
	private ChatSocketService clientes;

	@Override
	public void run(String... args) throws Exception {
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					var to = nombres.get(rnd.nextInt(nombres.size()));
					var text = frases.get(rnd.nextInt(frases.size()));
					clientes.broadcast(to + ": " + text);
					if (sendSTOMP != null)
						sendSTOMP.accept(to, text);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 0, 5000);
		remoto();
	}

	private static final String STOMP_SERVER_URL = "ws://localhost:8061/chat";

	private WebSocketStompClient stompClient;
	BiConsumer<String, String> sendSTOMP;

	public void remoto() throws Exception {
		WebSocketClient client = new StandardWebSocketClient();
		stompClient = new WebSocketStompClient(client);
		stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		stompClient.connectAsync(STOMP_SERVER_URL, new StompSessionHandlerAdapter() {
			private static final Logger log = LoggerFactory.getLogger("StompSessionHandlerAdapter");

			@Override
			public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
				session.subscribe("/topic/messages", this);
				sendSTOMP = (name, text) -> session.send("/example/chat", new MessageDto("[" + name + "]", text));
				sendSTOMP.accept("REMOTE", "Me he conectado!!!");
			}

			@Override
			public void handleException(StompSession session, StompCommand command, StompHeaders headers,
					byte[] payload, Throwable exception) {
				log.error("Got an exception", exception);
			}

			@Override
			public Type getPayloadType(StompHeaders headers) {
				return MessageDto.class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				MessageDto msg = (MessageDto) payload;
				try {
					clientes.broadcast("[STOMP " + msg.getName() + "]: " + msg.getText());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private Random rnd = new Random();
	private List<String> nombres = List.of("Hugo", "Mateo", "Martín", "Lucas", "Leo", "Lucía", "Sofía", "Martina",
			"María", "Julia", "Daniel", "Alejandro", "Manuel", "Pablo", "Álvaro", "Paula", "Valeria", "Emma", "Daniela",
			"Carla");
	private List<String> frases = List.of("Dime cómo te llamas y te pido para los Reyes.",
			"¿Tienes un diccionario? Me he quedado sin palabras al verte.",
			"Hola, ¿en qué parada debo bajarme para empezar a gustarte?",
			"¿Acabamos de subir al Hogwarts Express? Porque parece que nos dirigimos a un lugar mágico.",
			"Tus ojos son como IKEA. Estoy totalmente perdida en ellos.",
			"¿Eres electricista? Porque definitivamente acabas de iluminarme el día.",
			"Ni en los laboratorios existe tanta química como la que hay entre nosotros.",
			"Me gustas más que dormir hasta tarde.", "No soy un perro, pero 'guau' contigo.",
			"Si vas a estar en mi cabeza todo el día, al menos ponte ropa.",
			"Espero que sepas primeros auxilios, porque me has dejado sin respiración.",
			"En realidad no soy así de alto. Estoy sentado sobre mi cartera.",
			"Si las miradas pudieran matar, serías un arma de destrucción masiva.",
			"¿Te llamas WiFi? Porque realmente estoy sintiendo una conexión.",
			"¡Si tú y yo fuéramos calcetines, haríamos una gran pareja.",
			"¿Eres un préstamo bancario? ¡Porque tienes todo mi interés!", "Si yo fuese tú, estaría conmigo.",
			"Sabes que me gusta el café, pero hoy prefiero tener té.",
			"No contestaré a esa pregunta sin la presencia de mi abogado, que temo poder perder el juicio por ti.",
			"Seguramente no soy la persona más guapa del local, pero soy la única que se ha atrevido a hablarte.",
			"No sé lo que somos, pero no quiero que nunca lo dejemos de ser",
			"Parece que soy Google, solo me buscas cuando quieres algo.", "Quién fuera bizco para verte 2 veces.",
			"Oye, ¿te presentas a las elecciones? Porque eres un partidazo.",
			"He decidido gastar la batería de mi móvil contigo.",
			"Te invito al cine. Al menos si no te gusto habrás visto una buena peli.",
			"Estoy escribiendo una novela. Si me das tu número del móvil te la mando.",
			"Mi madre opina que deberías casarnos.", "Perdón, es que soy ecologista y quiero plantarte unos besos.",
			"Voy a estudiar Derecho para dar con el caso que no me haces");
}
