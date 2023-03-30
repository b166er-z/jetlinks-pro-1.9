package org.jetlinks.pro.messaging.rabbitmq;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SimpleAmqpMessage implements AmqpMessage {

    private String exchange;

    private String routeKey;

    private Map<String, Object> properties;

    private ByteBuf payload;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("exchange:").append(exchange);

        if (payload != null) {
            builder.append(",payload:");
            if (ByteBufUtil.isText(payload, StandardCharsets.UTF_8)) {
                builder.append(payload.toString(StandardCharsets.UTF_8));
            } else {
                builder.append(ByteBufUtil.hexDump(payload));
            }
        }
        return builder.toString();
    }
}
