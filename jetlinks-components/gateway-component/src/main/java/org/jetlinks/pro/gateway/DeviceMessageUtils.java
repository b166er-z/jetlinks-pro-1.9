package org.jetlinks.pro.gateway;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCountUtil;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class DeviceMessageUtils {


    @SuppressWarnings("all")
    public static Optional<DeviceMessage> convert(TopicPayload message) {
        return Optional.of(message.decode(DeviceMessage.class));
    }

    public static Optional<DeviceMessage> convert(ByteBuf payload) {
        try {
            return MessageType.convertMessage(JSON.parseObject(payload.toString(StandardCharsets.UTF_8)));
        }finally {
            ReferenceCountUtil.safeRelease(payload);
        }
    }

    public static void trySetProperties(DeviceMessage message, Map<String, Object> properties) {
        if (message instanceof ReportPropertyMessage) {
            ((ReportPropertyMessage) message).setProperties(properties);
        } else if (message instanceof ReadPropertyMessageReply) {
            ((ReadPropertyMessageReply) message).setProperties(properties);
        } else if (message instanceof WritePropertyMessageReply) {
            ((WritePropertyMessageReply) message).setProperties(properties);
        }
    }

    public static Optional<Map<String, Object>> tryGetProperties(DeviceMessage message) {

        if (message instanceof ReportPropertyMessage) {
            return Optional.ofNullable(((ReportPropertyMessage) message).getProperties());
        }

        if (message instanceof ReadPropertyMessageReply) {
            return Optional.ofNullable(((ReadPropertyMessageReply) message).getProperties());
        }
        if (message instanceof WritePropertyMessageReply) {
            return Optional.ofNullable(((WritePropertyMessageReply) message).getProperties());
        }
        return Optional.empty();
    }

}
