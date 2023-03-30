package org.jetlinks.pro.io.queue;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

/**
 * 持久化队列
 *
 * @author zhouhao
 * @since 1.8
 */
public interface PersistenceQueue {

    Mono<ByteBuffer> poll();

    Flux<ByteBuffer> take();

    Mono<Void> add(ByteBuffer byteBuffer);

    Mono<Void> add(String data);

}
