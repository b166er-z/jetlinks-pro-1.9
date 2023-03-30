package org.jetlinks.pro.messaging.kafka;

import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteBufferSerializer;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.ApplicationContext;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.internals.ConsumerFactory;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Slf4j
public class ReactorKafkaConsumer implements KafkaConsumer {

    private final KafkaProperties properties;

    private final Collection<String> topics;

    private volatile EmitterProcessor<Message> processor;

    private Disposable disposable;

    private final AtomicBoolean running = new AtomicBoolean();

    public ReactorKafkaConsumer(Collection<String> topics,
                                KafkaProperties properties) {
        Objects.requireNonNull(properties);
        Objects.requireNonNull(properties.getProducer());
        this.properties = properties;
        this.topics = topics;
        init();
    }

    private KafkaReceiver<byte[], byte[]> receiver;

    public void init() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());

        props.putAll(properties.getConsumer().buildProperties());
        props.putAll(properties.getProperties());

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        ReceiverOptions<byte[], byte[]> senderOptions = ReceiverOptions.<byte[], byte[]>create(props)
            .subscription(topics);
        receiver = KafkaReceiver.create(senderOptions);
    }

    protected void doStart() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        if (processor == null || processor.isCancelled()) {
            processor = EmitterProcessor.create();
        }
        FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

        disposable = receiver
            .receiveAutoAck()
            .flatMap(Function.identity())
            .<Message>map(record -> SimpleMessage.of(record.topic(),
                record.key() == null ? null : Unpooled.wrappedBuffer(record.key()),
                Unpooled.wrappedBuffer(record.value())))
            .doOnSubscribe(sub -> log.debug("subscribe kafka :{}", topics))
            .doOnCancel(() -> log.debug("unsubscribe kafka :{}", topics))
            .doOnNext(sink::next)
            .doOnComplete(sink::complete)
            .subscribe();

        sink.onDispose(() -> {
            running.set(false);
            disposable.dispose();
        });
    }

    @Override
    public Flux<Message> subscribe() {
        if (!running.get()) {
            doStart();
        }
        return processor;
    }

    @Override
    public void shutdown() {
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
        if (processor != null) {
            processor.onComplete();
            processor = null;
        }

    }
}
