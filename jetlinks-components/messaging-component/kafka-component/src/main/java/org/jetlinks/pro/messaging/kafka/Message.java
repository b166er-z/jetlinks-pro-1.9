package org.jetlinks.pro.messaging.kafka;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public interface Message {
    String getTopic();

    ByteBuf getKey();

    ByteBuf getPayload();

    default ByteBuffer keyToNio() {
        return getKey() == null ? null : getKey().nioBuffer();
    }

    default ByteBuffer payloadToNio() {
        return getPayload() == null ? null : getPayload().nioBuffer();
    }
}