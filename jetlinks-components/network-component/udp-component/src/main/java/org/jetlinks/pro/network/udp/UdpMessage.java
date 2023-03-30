package org.jetlinks.pro.network.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.TextMessageParser;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UdpMessage implements EncodedMessage {

    private ByteBuf payload;

    private InetSocketAddress address;

    public UdpMessage(ByteBuf payload) {
        this(payload, null);
    }

    /**
     * <pre>
     *     192.168.1.1:9000
     *
     *     {"message":"abc"}
     *
     * </pre>
     *
     * @param text 文本
     * @return UDP
     */
    public static UdpMessage of(String text) {

        UdpMessage message = new UdpMessage();

        TextMessageParser.of(
            first -> {
                String[] head = first.split("[ ]");
                String[] ipPort = (head.length > 1 ? head[1] : head[0]).split("[:]");
                if (ipPort.length < 2) {
                    throw new IllegalArgumentException("远程地址错误:" + first);
                }
                message.setAddress(new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])));
            },
            (head, val) -> {
            },
            body -> message.setPayload(Unpooled.wrappedBuffer(body.getBody())),
            () -> message.setPayload(Unpooled.wrappedBuffer(new byte[0]))
        ).parse(text);

        return message;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append(address).append("\n");

        if (ByteBufUtil.isText(payload, StandardCharsets.UTF_8)) {
            builder.append(payloadAsString());
        } else {
            ByteBufUtil.appendPrettyHexDump(builder, payload);
        }

        return builder.toString();
    }
}
