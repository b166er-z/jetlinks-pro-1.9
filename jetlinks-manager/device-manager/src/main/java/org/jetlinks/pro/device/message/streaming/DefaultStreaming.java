package org.jetlinks.pro.device.message.streaming;

import lombok.EqualsAndHashCode;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.HeaderKey;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.pro.streaming.Computer;
import org.jetlinks.pro.streaming.Streaming;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.function.Function;

@EqualsAndHashCode(of = "identity")
public class DefaultStreaming implements Streaming<DeviceMessage, Object, DeviceMessage> {

    private final EmitterProcessor<DeviceMessage> processor = EmitterProcessor.create(false);
    private final FluxSink<DeviceMessage> output = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private Disposable streaming;
    private FluxSink<DeviceMessage> input;
    private String property;
    private Object identity;

    public static final HeaderKey<String> virtualWindowProperty = HeaderKey.of("windowProperty", null, String.class);

    public static <V, T> DefaultStreaming create(Object identity,
                                                 String property,
                                                 Computer<DeviceMessage, V> computer,
                                                 Function<Flux<V>, Flux<T>> converter) {
        return DefaultStreaming
            .create(identity, property, flux -> flux
                .flatMap(computer::compute)
                .as(converter));
    }

    public static <T> DefaultStreaming create(Object identity,
                                              String property,
                                              Function<Flux<DeviceMessage>, Flux<T>> computer) {
        DefaultStreaming streaming = new DefaultStreaming();
        streaming.identity = identity;
        streaming.property = property;
        streaming.streaming = Flux
            .<DeviceMessage>create(sink -> streaming.input = sink)
            .groupBy(DeviceMessage::getDeviceId, Integer.MAX_VALUE)
            .flatMap(group -> {
                String deviceId = group.key();
                return computer
                    .apply(group)
                    .doOnNext(value -> {
                        if (value instanceof DeviceMessage) {
                            streaming.output.next(((DeviceMessage) value));
                        } else {
                            ReportPropertyMessage message = new ReportPropertyMessage();
                            //标记通过窗口函数产生的虚拟属性
                            message.addHeader(virtualWindowProperty, property);
                            message.setDeviceId(deviceId);
                            message.setProperties(Collections.singletonMap(property, value));
                            streaming.output.next(message);
                        }
                    })
                    .onErrorResume(err -> Mono.empty())
                    .then();
            }, Integer.MAX_VALUE)
            .subscribe();

        return streaming;
    }

    @Override
    public Mono<Object> compute(DeviceMessage data) {
        //来自同一个属性窗口则忽略计算,防止递归
        if (data.getHeader(virtualWindowProperty).map(property::equals).orElse(false)) {
            return Mono.empty();
        }
        if (processor.hasDownstreams()) {
            input.next(data);
        }
        return Mono.empty();
    }

    @Override
    public Flux<DeviceMessage> output() {
        return processor;
    }

    @Override
    public void dispose() {
        processor.onComplete();
        output.complete();
        streaming.dispose();
        input.complete();
    }

}
