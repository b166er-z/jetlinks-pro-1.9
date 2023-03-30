package org.jetlinks.pro.device.configuration;

import com.rabbitmq.client.ConnectionFactory;
import org.apache.kafka.clients.KafkaClient;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.pro.device.message.DeviceMessageConnector;
import org.jetlinks.pro.device.message.writer.KafkaMessageWriterConnector;
import org.jetlinks.pro.device.message.writer.RabbitMQMessageWriterConnector;
import org.jetlinks.pro.device.message.writer.TimeSeriesMessageWriterConnector;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.device.service.data.DeviceLatestDataService;
import org.jetlinks.pro.messaging.rabbitmq.ReactorRabbitMQConsumer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;

@Configuration
public class DeviceManagerConfiguration {

    @Bean
    public DeviceMessageConnector deviceMessageConnector(EventBus eventBus,
                                                         MessageHandler messageHandler,
                                                         DeviceSessionManager sessionManager,
                                                         DeviceLatestDataService dataService,
                                                         DeviceRegistry registry) {
        return new DeviceMessageConnector(eventBus, registry, messageHandler,dataService, sessionManager);
    }

    @Bean
    public TimeSeriesMessageWriterConnector timeSeriesMessageWriterConnector(DeviceDataService dataService) {
        return new TimeSeriesMessageWriterConnector(dataService);
    }

    @Configuration
    @ConditionalOnClass(KafkaClient.class)
    @EnableConfigurationProperties(KafkaProperties.class)
    @ConditionalOnProperty(prefix = "device.message.writer.kafka", name = "enabled", havingValue = "true")
    static class KafkaMessageWriterConnectorConfiguration{

        @Bean
        @ConfigurationProperties(prefix = "device.message.writer.kafka")
        public KafkaMessageWriterConnector kafkaMessageWriterConnector(DeviceDataService dataService,
                                                                       KafkaProperties kafkaProperties) {
            return new KafkaMessageWriterConnector(dataService, kafkaProperties);
        }

    }

    @Configuration
    @ConditionalOnClass(ConnectionFactory.class)
    @EnableConfigurationProperties(RabbitProperties.class)
    @ConditionalOnProperty(prefix = "device.message.writer.rabbitmq", name = "enabled", havingValue = "true")
    static class RabbitMQMessageWriterConnectorConfiguration{

        @Bean
        @ConfigurationProperties(prefix = "device.message.writer.rabbitmq")
        public RabbitMQMessageWriterConnector rabbitMQMessageWriterConnector(DeviceDataService dataService,
                                                                             Scheduler scheduler,
                                                                             RabbitProperties rabbitProperties) {
            RabbitMQMessageWriterConnector writerConnector = new RabbitMQMessageWriterConnector(dataService, rabbitProperties);
            writerConnector.setScheduler(scheduler);
            return writerConnector;
        }

    }

}
