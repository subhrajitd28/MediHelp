package com.medihelp.notification.config;

import com.medihelp.common.event.RabbitMQConfig;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
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

    // Queues
    @Bean
    public Queue welcomeQueue() {
        return QueueBuilder.durable(RabbitMQConfig.Q_NOTIFICATION_WELCOME).build();
    }

    @Bean
    public Queue medicationReminderQueue() {
        return QueueBuilder.durable(RabbitMQConfig.Q_NOTIFICATION_MEDICATION).build();
    }

    @Bean
    public Queue anomalyAlertQueue() {
        return QueueBuilder.durable(RabbitMQConfig.Q_NOTIFICATION_ANOMALY).build();
    }

    @Bean
    public Queue appointmentReminderQueue() {
        return QueueBuilder.durable(RabbitMQConfig.Q_NOTIFICATION_APPOINTMENT).build();
    }

    @Bean
    public Queue badgeEarnedQueue() {
        return QueueBuilder.durable(RabbitMQConfig.Q_NOTIFICATION_BADGE).build();
    }

    // Bindings
    @Bean
    public Binding welcomeBinding(Queue welcomeQueue, TopicExchange mediHelpExchange) {
        return BindingBuilder.bind(welcomeQueue).to(mediHelpExchange).with(RabbitMQConfig.RK_USER_REGISTERED);
    }

    @Bean
    public Binding medicationBinding(Queue medicationReminderQueue, TopicExchange mediHelpExchange) {
        return BindingBuilder.bind(medicationReminderQueue).to(mediHelpExchange).with(RabbitMQConfig.RK_MEDICATION_REMINDER);
    }

    @Bean
    public Binding anomalyBinding(Queue anomalyAlertQueue, TopicExchange mediHelpExchange) {
        return BindingBuilder.bind(anomalyAlertQueue).to(mediHelpExchange).with(RabbitMQConfig.RK_VITALS_ANOMALY);
    }

    @Bean
    public Binding appointmentBinding(Queue appointmentReminderQueue, TopicExchange mediHelpExchange) {
        return BindingBuilder.bind(appointmentReminderQueue).to(mediHelpExchange).with(RabbitMQConfig.RK_APPOINTMENT_REMINDER);
    }

    @Bean
    public Binding badgeBinding(Queue badgeEarnedQueue, TopicExchange mediHelpExchange) {
        return BindingBuilder.bind(badgeEarnedQueue).to(mediHelpExchange).with(RabbitMQConfig.RK_HEALTH_SCORE_UPDATED);
    }

    // Emergency SOS
    @Bean
    public Queue sosQueue() {
        return QueueBuilder.durable(RabbitMQConfig.Q_NOTIFICATION_SOS).build();
    }

    @Bean
    public Binding sosBinding(Queue sosQueue, TopicExchange mediHelpExchange) {
        return BindingBuilder.bind(sosQueue).to(mediHelpExchange).with(RabbitMQConfig.RK_EMERGENCY_SOS);
    }

    // JSON message converter with trusted packages
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("com.medihelp.common.event", "com.medihelp.common", "com.medihelp");
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
