package org.jetlinks.pro.messaging.kafka;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

@Getter
@AllArgsConstructor(staticName = "of")
public class SimpleMessage implements Message {

    private final String topic;

    private final ByteBuf key;

    private final ByteBuf payload;

    public static SimpleMessage of(String topic, String payload) {
        return new SimpleMessage(topic, null, Unpooled.wrappedBuffer(payload.getBytes()));
    }

    public static SimpleMessage of(String topic, Object payload) {
        return new SimpleMessage(topic, null, Unpooled.wrappedBuffer(JSON.toJSONBytes(payload)));
    }

    @Override
    public String toString() {

        String payloadString = ByteBufUtil.isText(payload, StandardCharsets.UTF_8)
            ? payload.toString(StandardCharsets.UTF_8)
            : ByteBufUtil.hexDump(payload);

        StringBuilder builder = new StringBuilder("Topic: ")
            .append("[").append(topic).append("]");

        if (key != null) {
            builder.append(", Key: ").append(key.toString(StandardCharsets.UTF_8));
        }

        return builder.append(", Payload: ")
            .append(payloadString)
            .toString()
            ;

    }
}
