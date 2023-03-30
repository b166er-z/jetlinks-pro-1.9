package org.jetlinks.pro.protocol;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.codec.*;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 根据消息类型来进行编码
 *
 * @author zhouhao
 * @since 1.0
 */
public class CustomDeviceMessageEncoder implements DeviceMessageEncoder {

    private final Map<MessageType, Function<MessageEncodeContext, Object>> encoders = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public Flux<? extends EncodedMessage> encode(MessageEncodeContext context) {
        return Mono.justOrEmpty(encoders.get(context.getMessage().getMessageType()))
                   .flatMapMany(func -> convert(func.apply(context)));
    }

    /**
     * 注册编码器
     *
     * @param type    对指定的消息类型进行编码
     * @param encoder 编码器
     * @return this
     */
    public CustomDeviceMessageEncoder encoder(String type, Function<MessageEncodeContext, Object> encoder) {
        return encoder(MessageType.valueOf(type.toUpperCase()), encoder);
    }

    /**
     * 注册编码器
     *
     * @param type    对指定的消息类型进行编码
     * @param encoder 编码器
     * @return this
     */
    public CustomDeviceMessageEncoder encoder(MessageType type, Function<MessageEncodeContext, Object> encoder) {
        encoders.put(type, encoder);
        return this;
    }

    @SuppressWarnings("all")
    protected Flux<EncodedMessage> convert(Object data) {

        if (data instanceof Publisher) {
            return Flux
                .from(((Publisher) data))
                .flatMap(this::convert);
        } else if (data instanceof Iterable) {
            return Flux.fromIterable(((Iterable) data))
                       .flatMap(this::convert);
        }

        if (data instanceof String) {
            data = ((String) data).getBytes();
        }
        if (data instanceof Map) {
            String topic = (String) ((Map) data).get("topic");
            if (StringUtils.hasText(topic)) {
                data = FastBeanCopier.copy(data, new SimpleMqttMessage());
            } else {
                data = JSON.toJSONBytes(data);
            }
        }

        if (data instanceof EncodedMessage) {
            return Flux.just(((EncodedMessage) data));
        } else if (data instanceof byte[]) {
            data = Unpooled.wrappedBuffer(((byte[]) data));
        }
        if (data instanceof ByteBuf) {
            return Flux.just(EncodedMessage.simple((ByteBuf) data));
        }
        Object d = data;
        return Flux.error(() -> new IllegalArgumentException("无法识别的消息格式:" + d));
    }
}
