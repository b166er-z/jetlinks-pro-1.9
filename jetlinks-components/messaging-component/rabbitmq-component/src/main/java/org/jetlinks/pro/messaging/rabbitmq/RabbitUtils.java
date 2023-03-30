package org.jetlinks.pro.messaging.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.BindingSpecification;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.Sender;

import java.time.Duration;

public class RabbitUtils {


    public static ConnectionFactory createConnectionFactory(RabbitProperties properties) {
        PropertyMapper map = PropertyMapper.get();
        RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
        map.from(properties::determineHost).whenNonNull().to(factory::setHost);
        map.from(properties::determinePort).to(factory::setPort);
        map.from(properties::determineUsername).whenNonNull().to(factory::setUsername);
        map.from(properties::determinePassword).whenNonNull().to(factory::setPassword);
        map.from(properties::determineVirtualHost).whenNonNull().to(factory::setVirtualHost);
        map.from(properties::getRequestedHeartbeat).whenNonNull().asInt(Duration::getSeconds)
            .to(factory::setRequestedHeartbeat);
        RabbitProperties.Ssl ssl = properties.getSsl();
        if (ssl.determineEnabled()) {
            factory.setUseSSL(true);
            map.from(ssl::getAlgorithm).whenNonNull().to(factory::setSslAlgorithm);
            map.from(ssl::getKeyStoreType).to(factory::setKeyStoreType);
            map.from(ssl::getKeyStore).to(factory::setKeyStore);
            map.from(ssl::getKeyStorePassword).to(factory::setKeyStorePassphrase);
            map.from(ssl::getTrustStoreType).to(factory::setTrustStoreType);
            map.from(ssl::getTrustStore).to(factory::setTrustStore);
            map.from(ssl::getTrustStorePassword).to(factory::setTrustStorePassphrase);
            map.from(ssl::isValidateServerCertificate)
                .to((validate) -> factory.setSkipServerCertificateValidation(!validate));
            map.from(ssl::getVerifyHostname).to(factory::setEnableHostnameVerification);
        }
        map.from(properties::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis)
            .to(factory::setConnectionTimeout);
        factory.setAutomaticRecoveryEnabled(true);

        factory.afterPropertiesSet();

        ConnectionFactory connectionFactory= factory.getRabbitConnectionFactory();
        connectionFactory.setNetworkRecoveryInterval(1000);
        return connectionFactory;
    }

    public static Mono<String> createTopicAndGroupQueue(Sender sender, String topic, String routeKey, String group) {
        String groupQueue = topic + ":" + group;
        return sender
            .declare(ExchangeSpecification.exchange(topic).durable(true).type("topic"))
            .then(sender.declareQueue(QueueSpecification.queue(topic + ":" + group).durable(true)))
            .then(sender.bindQueue(BindingSpecification.binding(topic, routeKey, groupQueue)))
            .thenReturn(groupQueue);

    }
}
