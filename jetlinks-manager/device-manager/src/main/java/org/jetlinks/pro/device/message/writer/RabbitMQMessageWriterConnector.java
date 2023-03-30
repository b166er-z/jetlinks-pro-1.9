package org.jetlinks.pro.device.message.writer;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.ConnectionFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.gateway.DeviceMessageUtils;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.messaging.rabbitmq.*;
import org.jetlinks.pro.utils.MessageTypeMatcher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class RabbitMQMessageWriterConnector implements CommandLineRunner {

    private RabbitMQProducer producer;

    private RabbitMQConsumer mqConsumer;

    private final DeviceDataService dataService;

    @Getter
    @Setter
    private String topicName = "device.message";

    @Setter
    @Getter
    private String consumerRouteKey = "";

    @Setter
    @Getter
    private String producerRouteKey = "";

    @Getter
    @Setter
    private boolean consumer = true;

    @Getter
    @Setter
    private String group = "default";

    @Getter
    @Setter
    private boolean autoAck = true;

    @Getter
    @Setter
    private int threadSize = 4;

    private final RabbitProperties properties;

    @Getter
    @Setter
    private MessageTypeMatcher type = new MessageTypeMatcher();

    @Setter
    @Getter
    private Scheduler scheduler = Schedulers.parallel();

    public RabbitMQMessageWriterConnector(DeviceDataService dataService,
                                          RabbitProperties properties) {
        this.properties = properties;
        this.dataService = dataService;
    }

    @Subscribe(topics = "/device/**", id = "device-message-rabbitmq-writer")
    public Mono<Void> writeDeviceMessageToTs(TopicPayload payload) {

        DeviceMessage message = payload.decode(DeviceMessage.class);

        if (!type.match(message.getMessageType())) {
            payload.release();
            return Mono.empty();
        }
        ByteBuf messageBuf = Unpooled.wrappedBuffer(JSON.toJSONBytes(message.toJson()));
        return producer
            .publish(SimpleAmqpMessage.of(topicName, producerRouteKey, null, messageBuf))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @PostConstruct
    public void init() {
        ConnectionFactory connectionFactory = RabbitUtils.createConnectionFactory(properties);
        connectionFactory.useNio();

        this.producer = new ReactorRabbitMQProducer(connectionFactory).init();
        if (consumer) {
            this.mqConsumer = new ReactorRabbitMQConsumer(topicName, true, connectionFactory)
                .consumerGroup(group)
                .consumerRouteKey(consumerRouteKey)
                .consumerThread(threadSize)
                .autoAck(autoAck)
                .init();
        }
    }

    private final Disposable.Composite disposable = Disposables.composite();

    @PreDestroy
    public void shutdown() {
        this.producer.shutdown();
        if (this.mqConsumer != null) {
            this.mqConsumer.shutdown();
        }
        disposable.dispose();
    }

    @Override
    public void run(String... args) {
        if (!consumer || this.mqConsumer == null) {
            return;
        }
        disposable
            .add(mqConsumer
                     .subscribe()
                     .bufferTimeout(3000, Duration.ofSeconds(3), () -> new ArrayList<>(3000))
                     .flatMap(list -> Flux
                         .fromIterable(list)
                         .flatMap(ms -> Mono.justOrEmpty(DeviceMessageUtils.convert(ms.getPayload())))
                         .as(dataService::saveDeviceMessage)
                         .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                         .thenReturn(true)
                         .onErrorResume(err -> {
                             log.error("write device data error", err);
                             return Mono.just(false);
                         })
                         .doOnSuccess((r) -> {
                             if (r) {
                                 log.debug("write device message [{}] success", list.size());
                             }
                             for (AmqpMessage amqpMessage : list) {
                                 if (!(amqpMessage instanceof AcknowledgableMessage)) {
                                     return;
                                 }
                                 if (r) {
                                     ((AcknowledgableMessage) amqpMessage).ack();
                                 } else {
                                     ((AcknowledgableMessage) amqpMessage).nack(true);
                                 }
                             }
                         }), Integer.MAX_VALUE
                     )
                     .doOnCancel(() -> {
                         //断掉了,重新消费.
                         if (!disposable.isDisposed()) {
                             run(args);
                         }
                     })
                     .subscribe()
            );
    }
}
