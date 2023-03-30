package org.jetlinks.pro.protocol;

import lombok.AllArgsConstructor;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.*;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;

/**
 * 组合编解码器，将encoder和decoder组合为编解码器
 *
 * @author zhouhao
 * @since 1.0
 */
@AllArgsConstructor
public class CompositeDeviceMessageCodec implements DeviceMessageCodec {

    private final Transport transport;

    private final DeviceMessageEncoder encoder;

    private final DeviceMessageDecoder decoder;

    @Override
    public Transport getSupportTransport() {
        return transport;
    }

    @Nonnull
    @Override
    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        return decoder.decode(context);
    }

    @Nonnull
    @Override
    public Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
        return encoder.encode(context);
    }
}
