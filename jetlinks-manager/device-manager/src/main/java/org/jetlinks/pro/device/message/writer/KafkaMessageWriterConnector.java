package org.jetlinks.pro.device.message.writer;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.gateway.DeviceMessageUtils;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.messaging.kafka.*;
import org.jetlinks.pro.utils.MessageTypeMatcher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;

@Slf4j
public class KafkaMessageWriterConnector implements CommandLineRunner {

    private final KafkaProducer producer;

    private final KafkaConsumer kafkaConsumer;

    private final DeviceDataService dataService;

    @Getter
    @Setter
    private String topicName = "device.message";

    @Getter
    @Setter
    private boolean consumer = true;

    @Getter
    @Setter
    private MessageTypeMatcher type = new MessageTypeMatcher();

    public KafkaMessageWriterConnector(DeviceDataService dataService,
                                       KafkaProperties properties) {
        this.producer = new ReactorKafkaProducer(properties);
        this.kafkaConsumer = new ReactorKafkaConsumer(Collections.singleton(topicName), properties);
        this.dataService = dataService;
    }

    @Subscribe(topics = "/device/**", id = "device-message-kafka-writer")
    public Mono<Void> writeDeviceMessageToTs(TopicPayload payload) {

        ByteBuf topic = Unpooled.wrappedBuffer(payload.getTopic().getBytes());
        DeviceMessage message = payload.decode(DeviceMessage.class);
        ByteBuf messageBuf = Unpooled.wrappedBuffer(JSON.toJSONBytes(message.toJson()));
        if (!type.match(message.getMessageType())) {
            return Mono.empty();
        }

        return producer
            .send(Mono.just(SimpleMessage.of(topicName, topic, messageBuf)))
            .subscribeOn(Schedulers.elastic());
    }

    @Override
    public void run(String... args) {
        if (!consumer) {
            return;
        }
        kafkaConsumer
            .subscribe()
            .flatMap(msg -> Mono.justOrEmpty(DeviceMessageUtils.convert(msg.getPayload())))
            .bufferTimeout(3000, Duration.ofSeconds(3))
            .publishOn(Schedulers.parallel())
            .flatMap(list -> dataService
                .saveDeviceMessage(list)
                .then(Mono.fromRunnable(() -> {
                    log.debug("write device message [{}] success", list.size());
                }))
                .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(2)))
                .onErrorResume((err) -> {
                    log.error("write device data error", err);
                    return Mono.empty();
                })
            )
            .subscribe();
    }
}
