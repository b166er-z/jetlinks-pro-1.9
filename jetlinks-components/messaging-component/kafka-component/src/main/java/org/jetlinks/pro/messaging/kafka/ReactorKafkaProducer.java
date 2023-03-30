package org.jetlinks.pro.messaging.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteBufferSerializer;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ReactorKafkaProducer implements KafkaProducer {

    private final KafkaProperties properties;

    public ReactorKafkaProducer(KafkaProperties properties) {
        Objects.requireNonNull(properties);
        Objects.requireNonNull(properties.getProducer());
        this.properties = properties;
        init();
    }

    private KafkaSender<ByteBuffer, ByteBuffer> sender;

    private void init() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getClientId());

        props.putAll(properties.getProducer().buildProperties());
        props.putAll(properties.getProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteBufferSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteBufferSerializer.class);

        SenderOptions<ByteBuffer, ByteBuffer> senderOptions = SenderOptions.create(props);

        sender = KafkaSender.create(senderOptions);
    }

    @Override
    public Mono<Void> send(Publisher<Message> publisher) {
        if (sender == null) {
            return Mono.error(new IllegalStateException("kafka sender is shutdown"));
        }
        return sender
            .send(Flux
                .from(publisher)
                .map(msg -> SenderRecord
                    .create(new ProducerRecord<>(msg.getTopic(), msg.keyToNio(), msg.payloadToNio()), msg))
            )
            .flatMap(result -> {
                if (null != result.exception()) {
                    return Mono.error(result.exception());
                }
                if (log.isDebugEnabled()) {
                    RecordMetadata metadata = result.recordMetadata();
                    log.debug("Kafka Message {} sent successfully, topic-partition={}-{} offset={} timestamp={}",
                        result.correlationMetadata(),
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset(),
                        metadata.timestamp());
                }
                return Mono.empty();
            })
            .then();
    }

    @Override
    public void shutdown() {
        if (null != sender) {
            sender.close();
        }
        sender = null;
    }
}
