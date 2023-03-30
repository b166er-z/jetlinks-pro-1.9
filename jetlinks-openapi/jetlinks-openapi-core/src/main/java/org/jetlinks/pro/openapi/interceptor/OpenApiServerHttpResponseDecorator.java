package org.jetlinks.pro.openapi.interceptor;

import org.apache.commons.codec.binary.Hex;
import org.jetlinks.pro.openapi.OpenApiClient;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("all")
public class OpenApiServerHttpResponseDecorator extends ServerHttpResponseDecorator {


    public OpenApiServerHttpResponseDecorator(ServerHttpResponse delegate) {
        super(delegate);
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        return Mono.subscriberContext()
                .flatMap(ctx -> Mono
                        .justOrEmpty(ctx.<OpenApiClient>getOrEmpty(OpenApiClient.class))
                        .flatMap(client -> Flux.from(body)
                                .collectList()
                                .flatMap(list -> {
                                    doSign(client, list);
                                    return super.writeWith(Flux.fromIterable(list));
                                }))
                        .switchIfEmpty(Mono.defer(() -> super.writeWith(body))));

    }

    @Override
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        return Mono.subscriberContext()
                .flatMap(ctx -> Mono
                        .justOrEmpty(ctx.<OpenApiClient>getOrEmpty(OpenApiClient.class))
                        .flatMap(client -> Flux.from(body)
                                .flatMap(Function.identity())
                                .collectList()
                                .flatMap(list -> {
                                    doSign(client, list);
                                    return super.writeWith(Flux.fromIterable(list));
                                }))
                        .switchIfEmpty(Mono.defer(() -> super.writeAndFlushWith(body))));
    }

    private void doSign(OpenApiClient client, List<? extends DataBuffer> buffers) {
        MessageDigest digest = client.getSignature().getMessageDigest();
        for (DataBuffer dataBuffer : buffers) {
            digest.update(dataBuffer.asByteBuffer());
        }
        String time = String.valueOf(System.currentTimeMillis());
        digest.update(time.getBytes());
        digest.update(client.getSecureKey().getBytes());

        getHeaders().add("X-Timestamp", time);
        getHeaders().add("X-Sign", Hex.encodeHexString(digest.digest()));
    }

}
