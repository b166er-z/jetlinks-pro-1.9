package org.jetlinks.pro.openapi.interceptor;

import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.SneakyThrows;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static reactor.core.scheduler.Schedulers.single;

public class OpenApiServerHttpRequestDecorator extends ServerHttpRequestDecorator {


    public OpenApiServerHttpRequestDecorator(ServerHttpRequest delegate) {
        super(delegate);
    }

    private final AtomicBoolean already = new AtomicBoolean();

    private static final DataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));

    private final List<byte[]> payload = new CopyOnWriteArrayList<>();

    @Override
    public Flux<DataBuffer> getBody() {
        if (!already.getAndSet(true)) {
            Flux<DataBuffer> flux = super.getBody();
            return flux
                .map(this::cache);
        } else {
            return Flux.fromIterable(payload)
                .map(nettyDataBufferFactory::wrap);
        }
    }

    @SneakyThrows
    private DataBuffer cache(DataBuffer buffer) {
        byte[] cache = StreamUtils.copyToByteArray(buffer.asInputStream());
        payload.add(cache);
        return nettyDataBufferFactory.wrap(cache);

    }

}
