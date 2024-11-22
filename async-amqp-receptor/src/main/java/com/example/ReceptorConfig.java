package com.example;

import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;

import com.example.models.MessageDTO;
import com.example.models.Store;
import com.rabbitmq.client.Channel;

@Configuration
public class ReceptorConfig {
    private static final Logger LOGGER = Logger.getLogger(ReceptorConfig.class.getName());
    
    private static boolean pausa=false;
    public static boolean cambia() {
    	return pausa = !pausa;		
	}
    
    @RabbitListener(queues = "${app.cola}")
    public void listener(MessageDTO in, Channel channel) throws InterruptedException {
    	if(pausa) {
    		throw new ImmediateRequeueAmqpException("En pausa");
    	}
    	if(in.getMsg() == null) {
    		throw new AmqpRejectAndDontRequeueException("Mensaje invalido.");
    	}
    	Store.add(in);
    	Thread.sleep(in.getMsg().length() * 500);
    	LOGGER.warning("RECIBIDO: " + in);
    }
    
    @RabbitListener(queues = "#{'${app.idiomas.colas}'.split(',')}")
    public void listenerMulticola(MessageDTO in, @Header("lang") String lang, 
    		org.springframework.amqp.core.Message messageRaw) throws InterruptedException {
    	if(in.getMsg() == null) {
    		throw new AmqpRejectAndDontRequeueException("Mensaje invalido.");
    	}
    	in.setMsg(String.format("Original: %s Lang: %s Cola: %s", in.getMsg(), lang, messageRaw.getMessageProperties().getConsumerQueue()));
    	Store.add(in);
    	LOGGER.warning("RECIBIDO: " + in);
       	LOGGER.warning("RAW: " + messageRaw);
    }
    
    @RabbitListener(queues = "#{'${app.topic.colas}'.split(',')}")
    public void listenerTema(MessageDTO in, org.springframework.amqp.core.Message messageRaw) throws InterruptedException {
    	if(in.getMsg() == null) {
    		throw new AmqpRejectAndDontRequeueException("Mensaje invalido.");
    	}
    	in.setMsg(String.format("Mensaje: %s Tema: %s Cola: %s", in.getMsg(), messageRaw.getMessageProperties().getReceivedRoutingKey(), messageRaw.getMessageProperties().getConsumerQueue()));
    	Store.add(in);
    	LOGGER.warning("RECIBIDO: " + in);
    }

	@Value("${spring.application.name}:${server.port}")
	private String origen;

    private Random rnd = new Random();
    
    @RabbitListener(queues = "${app.rpc.queue}")
    public MessageDTO responde(MessageDTO in) throws InterruptedException {
    	LOGGER.warning("SOLICITUD RECIBIDA: " + in);
    	Thread.sleep(in.getMsg().length() * 500);
    	LOGGER.warning("RESPONDIENDO EN: " + new Date());
    	var out = new MessageDTO((rnd.nextInt(2) == 0 ? "ACEPTADA -> " : "RECHAZADA -> ") + in.toString(), origen);
    	out.setId(in.getId());
    	return out;
    }

    @RabbitListener(queues = "demos.coreografia.paso1")
    @SendTo("demos.coreografia.paso2")
    public MessageDTO listenerPaso1(MessageDTO in, Channel channel) throws InterruptedException {
    	if(pausa) {
    		throw new ImmediateRequeueAmqpException("En pausa");
    	}
    	if(in.getMsg() == null) {
    		throw new AmqpRejectAndDontRequeueException("Mensaje invalido.");
    	}
    	in.setMsg(in.getMsg() + " -> paso 1 (" + origen +")");
    	Thread.sleep(500);
    	LOGGER.warning("PASO: " + in.getMsg());
    	return in;
    }


    @RabbitListener(queues = "demos.coreografia.paso3")
    @SendTo("demos.coreografia.paso4")
    public MessageDTO listenerPaso3(MessageDTO in, Channel channel) throws InterruptedException {
    	in.setMsg(in.getMsg() + " -> paso 3 (" + origen +")");
    	Thread.sleep(500);
    	LOGGER.warning("PASO: " + in.getMsg());
    	return in;
    }

    @RabbitListener(queues = "demos.orquetacion.pasoA")
    public MessageDTO respondeA(MessageDTO in) throws InterruptedException {
    	in.setMsg(in.getMsg() + " -> paso A (" + origen +")");
    	return in;
    }
    @RabbitListener(queues = "demos.orquetacion.pasoC")
    public MessageDTO respondeB(MessageDTO in) throws InterruptedException {
    	in.setMsg(in.getMsg() + " -> paso C (" + origen +")");
    	return in;
    }

}
