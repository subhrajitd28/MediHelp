package com.medihelp.auth.config;

import com.medihelp.common.event.RabbitMQConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange mediHelpExchange() {
        return new TopicExchange(RabbitMQConfig.EXCHANGE);
    }

    @Bean
    public Queue profileUserRegisteredQueue() {
        return new Queue(RabbitMQConfig.Q_PROFILE_USER_REGISTERED, true);
    }

    @Bean
    public Queue notificationWelcomeQueue() {
        return new Queue(RabbitMQConfig.Q_NOTIFICATION_WELCOME, true);
    }

    @Bean
    public Binding profileBinding(TopicExchange mediHelpExchange, Queue profileUserRegisteredQueue) {
        return BindingBuilder.bind(profileUserRegisteredQueue)
                .to(mediHelpExchange)
                .with(RabbitMQConfig.RK_USER_REGISTERED);
    }

    @Bean
    public Binding welcomeBinding(TopicExchange mediHelpExchange, Queue notificationWelcomeQueue) {
        return BindingBuilder.bind(notificationWelcomeQueue)
                .to(mediHelpExchange)
                .with(RabbitMQConfig.RK_USER_REGISTERED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
