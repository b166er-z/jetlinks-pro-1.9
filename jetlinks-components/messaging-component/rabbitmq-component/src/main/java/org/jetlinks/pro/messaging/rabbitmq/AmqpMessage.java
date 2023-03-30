package org.jetlinks.pro.messaging.rabbitmq;

import io.netty.buffer.ByteBuf;

import java.util.Map;

public interface AmqpMessage {

    String getExchange();

    String getRouteKey();

    Map<String,Object> getProperties();

    ByteBuf getPayload();

}
