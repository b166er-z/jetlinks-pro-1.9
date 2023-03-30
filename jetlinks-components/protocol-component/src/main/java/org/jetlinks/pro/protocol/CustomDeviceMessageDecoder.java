package org.jetlinks.pro.protocol;

import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.codec.*;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Function;

/**
 * 使用指定的函数进行解码，并将返回值转换为对应的消息
 *
 * @author zhouhao
 * @since 1.0
 */
public class CustomDeviceMessageDecoder implements DeviceMessageDecoder {

    private Function<MessageDecodeContext, Object> decoder;

    public void decoder(Function<MessageDecodeContext, Object> decoder) {
        this.decoder = decoder;
    }

    @Override
    @Nonnull
    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        if (decoder == null) {
            return Mono.error(new UnsupportedOperationException("decoder can not be null"));
        }
        return convert(decoder.apply(context));
    }

    @SuppressWarnings("all")
    protected Publisher<? extends Message> convert(Object object) {
        if (StringUtils.isEmpty(object)) {
            return Mono.empty();
        }

        if (object instanceof Map) {
            return Mono.justOrEmpty(MessageType.convertMessage(((Map) object)))
                       .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("无法转换设备消息:" + object)));
        }

        if (object instanceof Object[]) {
            return Flux.just(((Object[]) object))
                       .flatMap(this::convert);
        }
        if (object instanceof Iterable) {
            return Flux.fromIterable(((Iterable) object))
                       .flatMap(this::convert);
        }

        if (object instanceof Publisher) {
            return Flux.from(((Publisher) object))
                       .flatMap(this::convert);
        }

        return Flux.error(new UnsupportedOperationException("无法转换设备消息:" + object));


    }

}
