package org.jetlinks.pro.network.tcp.parser.strateies;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.network.tcp.parser.PayloadParser;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <pre>
 * PipePayloadParser parser = new PipePayloadParser();
 * //先读取4个字节
 * parser.fixed(4)
 *        //第一次读取数据
 *       .handler(buffer -> {
 *            //4字节转为int，表示接下来要读取的包长度
 *            int len = BytesUtils.beToInt(buffer.getBytes());
 *            parser
 *            .result(buffer) //将已读取的4字节设置到结果中
 *            .fixed(len);//设置接下来要读取的字节长度
 *         })
 *         //第二次读取数据
 *       .handler(buffer -> parser
 *                .result(buffer) //设置结果
 *                .complete() //完成本次读取，输出结果，开始下一次读取
 *                );
 * </pre>
 */
@Slf4j
public class PipePayloadParser implements PayloadParser {

    private final EmitterProcessor<Buffer> processor = EmitterProcessor.create(true);

    private final FluxSink<Buffer> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final List<Consumer<Buffer>> pipe = new CopyOnWriteArrayList<>();

    private final List<Buffer> result = new CopyOnWriteArrayList<>();

    private volatile RecordParser recordParser;

    private Function<Buffer, Buffer> directMapper;

    private Consumer<RecordParser> firstInit;

    private final AtomicInteger currentPipe = new AtomicInteger();

    public PipePayloadParser result(String buffer) {
        return result(Buffer.buffer(buffer));
    }

    public PipePayloadParser result(byte[] buffer) {
        return result(Buffer.buffer(buffer));
    }

    public PipePayloadParser handler(Consumer<Buffer> handler) {
        pipe.add(handler);
        return this;
    }

    public PipePayloadParser delimited(String delimited) {
        if (recordParser == null) {
            setParser(RecordParser.newDelimited(delimited));
            firstInit = (parser -> parser.delimitedMode(delimited));
            return this;
        }
        recordParser.delimitedMode(delimited);
        return this;
    }

    public PipePayloadParser fixed(int size) {
        if (size == 0) {
            complete();
            return this;
        }
        if (recordParser == null) {
            setParser(RecordParser.newFixed(size));
            firstInit = (parser -> parser.fixedSizeMode(size));
            return this;
        }
        recordParser.fixedSizeMode(size);
        return this;
    }

    public PipePayloadParser direct(Function<Buffer, Buffer> mapper) {
        this.directMapper = mapper;
        return this;
    }

    private Consumer<Buffer> getNextHandler() {
        int i = currentPipe.getAndIncrement();
        if (i < pipe.size()) {
            return pipe.get(i);
        }
        currentPipe.set(0);
        return pipe.get(0);
    }

    private void setParser(RecordParser parser) {
        this.recordParser = parser;
        this.recordParser.handler(buffer -> getNextHandler().accept(buffer));
    }

    public PipePayloadParser complete() {
        currentPipe.set(0);
        if (recordParser != null) {
            firstInit.accept(recordParser);
        }
        if (!this.result.isEmpty()) {
            Buffer buffer = Buffer.buffer();
            for (Buffer buf : this.result) {
                buffer.appendBuffer(buf);
            }
            this.result.clear();
            sink.next(buffer);
        }
        return this;

    }

    public PipePayloadParser result(Buffer buffer) {
        this.result.add(buffer);
        return this;
    }

    @Override
    public synchronized void handle(Buffer buffer) {
        if (recordParser == null && directMapper == null) {
            log.error("record parser not init");
            return;
        }
        if (recordParser != null) {
            recordParser.handle(buffer);
            return;
        }
        Buffer buf = directMapper.apply(buffer);
        if (null != buf) {
            sink.next(buf);
        }
    }

    @Override
    public Flux<Buffer> handlePayload() {
        return processor;
    }

    @Override
    public void reset() {
        this.result.clear();
        complete();
    }

    @Override
    public void close() {
        processor.onComplete();
        currentPipe.set(0);
        this.result.clear();
    }

}
