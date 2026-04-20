package com.aisale.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CHAT_QUEUE = "chat.message.queue";
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String CHAT_ROUTING_KEY = "chat.message";

    @Bean
    public Queue chatQueue() {
        return new Queue(CHAT_QUEUE, true); // 持久化队列
    }

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_EXCHANGE);
    }

    @Bean
    public Binding chatBinding() {
        return BindingBuilder.bind(chatQueue())
            .to(chatExchange())
            .with(CHAT_ROUTING_KEY);
    }
}