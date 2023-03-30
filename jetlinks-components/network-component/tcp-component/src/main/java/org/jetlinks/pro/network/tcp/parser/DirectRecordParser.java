package org.jetlinks.pro.network.tcp.parser;

import io.vertx.core.buffer.Buffer;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.function.Function;

/**
 * 不处理直接返回数据包
 *
 * @author zhouhao
 * @since 1.0
 */
public class DirectRecordParser implements PayloadParser {

    private final EmitterProcessor<Buffer> processor = EmitterProcessor.create(false);

    private final FluxSink<Buffer> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    @Override
    public void handle(Buffer buffer) {
        sink.next(buffer);
    }

    @Override
    public Flux<Buffer> handlePayload() {
        return processor;
    }

    @Override
    public void close() {
        processor.onComplete();
    }
}
