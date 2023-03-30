package org.jetlinks.pro.messaging.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import io.netty.buffer.ByteBufUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ReactorRabbitMQProducer implements RabbitMQProducer {

    private RabbitProperties properties;

    private final SendOptions options = new SendOptions();

    protected Sender sender;

    private final EmitterProcessor<OutboundMessage> msgProcessor = EmitterProcessor.create(false);

    private final FluxSink<OutboundMessage> messageSink = msgProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final AtomicBoolean fast = new AtomicBoolean();

    private final ConnectionFactory connectionFactory;

    @Setter
    private int maxBatchSize = Integer.getInteger("jetlinks.rabbitmq.batch.max.size", 3000);

    public ReactorRabbitMQProducer(RabbitProperties properties) {
        this.properties = properties;
        this.connectionFactory = RabbitUtils.createConnectionFactory(properties);
        this.connectionFactory.setAutomaticRecoveryEnabled(true);
        this.connectionFactory.setNetworkRecoveryInterval(1000);
        connectionFactory.useNio();
    }

    public ReactorRabbitMQProducer(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public ReactorRabbitMQProducer(Sender sender, ConnectionFactory connectionFactory) {
        this.sender = sender;
        this.connectionFactory = connectionFactory;
    }

    public ReactorRabbitMQProducer init() {
        if (this.sender != null && this.properties == null) {
            return this;
        }
        SenderOptions senderOptions = new SenderOptions()
            .connectionFactory(connectionFactory)
            .resourceManagementScheduler(Schedulers.boundedElastic());
        sender = RabbitFlux.createSender(senderOptions);
        return this;
    }


    private void startFastSend() {
        if (fast.compareAndSet(false, true)) {
            log.debug("start rabbitMQ fast send processor");
            msgProcessor
                .windowTimeout(maxBatchSize, Duration.ofSeconds(5))
                .flatMap(flux -> sender
                    .send(flux, options)
                    .onErrorResume(err -> Mono.fromRunnable(() -> log.error(err.getMessage(), err)))
                ).doFinally(f -> fast.set(false))
                .subscribe();
        }
    }

    @Override
    public Mono<Void> publish(AmqpMessage message) {
        return Mono
            .fromRunnable(() -> messageSink.next(createMessage(message)))
            .doOnSubscribe(sub -> startFastSend())
            .then();
    }

    @Override
    public Mono<Void> publish(Publisher<AmqpMessage> amqpMessageStream) {
        return Flux.from(amqpMessageStream)
            .map(this::createMessage)
            .as(flux -> sender.send(flux, options));
    }

    protected OutboundMessage createMessage(AmqpMessage message) {
        return new OutboundMessage(message.getExchange(), message.getRouteKey(), ByteBufUtil.getBytes(message.getPayload()));
    }

    @Override
    public void shutdown() {
        messageSink.complete();
        msgProcessor.onComplete();
        sender.close();
    }
}
