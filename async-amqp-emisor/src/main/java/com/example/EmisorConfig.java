package com.example;

import java.util.logging.Logger;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.SendTo;

import com.example.models.MessageDTO;
import com.example.resources.EmisorResource;
import com.rabbitmq.client.Channel;

@Configuration
public class EmisorConfig {
	private static final Logger LOGGER = Logger.getLogger(EmisorConfig.class.getName());
   
	@Bean
	AsyncRabbitTemplate asyncRabbitTemplate(RabbitTemplate template) {
		return new AsyncRabbitTemplate(template);
	}

	@Value("${spring.application.name}:${server.port}")
	private String origen;

    @RabbitListener(queues = "demos.coreografia.paso2")
    @SendTo("demos.coreografia.paso3")
    public MessageDTO listenerPaso2(MessageDTO in, Channel channel) throws InterruptedException {
    	in.setMsg(in.getMsg() + " -> paso 2 (" + origen +")");
    	Thread.sleep(500);
    	LOGGER.warning("PASO: " + in.getMsg());
    	return in;
    }

    @RabbitListener(queues = "demos.coreografia.paso4")
    public void listenerPaso4(MessageDTO in, Channel channel) throws InterruptedException {
    	in.setMsg(in.getMsg() + " -> proceso concluido (" + origen + ")");
    	LOGGER.warning("PASO: " + in.getMsg());
    }

    @RabbitListener(queues = "demos.orquetacion.pasoB")
    public MessageDTO respondeB(MessageDTO in) throws InterruptedException {
    	in.setMsg(in.getMsg() + " -> paso B (" + origen +")");
    	return in;
    }

}
