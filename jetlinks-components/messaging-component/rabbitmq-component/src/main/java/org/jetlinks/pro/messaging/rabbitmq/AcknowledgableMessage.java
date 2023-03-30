package org.jetlinks.pro.messaging.rabbitmq;

public interface AcknowledgableMessage extends AmqpMessage {

    void ack();

    void nack(boolean requeue);
}
