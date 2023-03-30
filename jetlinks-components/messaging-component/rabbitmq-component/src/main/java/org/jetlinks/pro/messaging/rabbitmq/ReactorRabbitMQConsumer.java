package org.jetlinks.pro.messaging.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ReactorRabbitMQConsumer implements RabbitMQConsumer {

    private boolean autoAck = true;

    protected Receiver receiver;

    @Setter
    private ConsumeOptions options = new ConsumeOptions().qos(3000);

    private int consumerThreadSize = Integer.getInteger("jetlinks.rabbitmq.consumer.thread", 4);

    private final String queue;

    private String subscribeQueue;

    private String group = System.getProperty("jetlinks.rabbitmq.consumer.group", "default");

    private String routeKey = "";

    private final boolean topic;

    @Setter
    private boolean autoCreateTopic = true;

    private volatile EmitterProcessor<AmqpMessage> processor;

    private final AtomicBoolean running = new AtomicBoolean();

    private final ConnectionFactory connectionFactory;

    public ReactorRabbitMQConsumer(String exchangeOrQueue, boolean topic, RabbitProperties properties) {
        this(exchangeOrQueue, topic, RabbitUtils.createConnectionFactory(properties));
    }

    public ReactorRabbitMQConsumer(String exchangeOrQueue, boolean topic, ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.queue = this.subscribeQueue = exchangeOrQueue;
        this.topic = topic;
    }

    public ReactorRabbitMQConsumer(String exchangeOrQueue, boolean topic, Receiver receiver, ConnectionFactory connectionFactory) {
        this.receiver = receiver;
        this.queue = this.subscribeQueue = exchangeOrQueue;
        this.topic = topic;
        this.connectionFactory = connectionFactory;
    }

    public ReactorRabbitMQConsumer init() {
        if (this.receiver == null) {
            ReceiverOptions options = new ReceiverOptions()
                .connectionFactory(connectionFactory);
            receiver = RabbitFlux.createReceiver(options);
        }

        if (topic && autoCreateTopic) {
            try (Sender sender = RabbitFlux.createSender(new SenderOptions().connectionFactory(connectionFactory))) {
                RabbitUtils
                    .createTopicAndGroupQueue(sender, queue, routeKey, group)
                    .doOnError(err -> log.error(err.getMessage(), err))
                    .doOnSuccess(queue -> this.subscribeQueue = queue)
                    .block();
            }
        }
        return this;
    }

    public ReactorRabbitMQConsumer consumerGroup(String group) {
        this.group = group;
        return this;
    }

    public ReactorRabbitMQConsumer consumerRouteKey(String routeKey) {
        this.routeKey = routeKey;

        return this;
    }

    public ReactorRabbitMQConsumer consumerThread(int consumerThreadSize) {
        this.consumerThreadSize = consumerThreadSize;
        return this;
    }

    public ReactorRabbitMQConsumer autoCreateTopic(boolean autoCreateTopic) {
        this.autoCreateTopic = autoCreateTopic;

        return this;
    }

    public ReactorRabbitMQConsumer autoAck(boolean autoAck) {
        this.autoAck = autoAck;
        return this;
    }

    protected AmqpMessage createMessage(AcknowledgableDelivery delivery) {
        if (autoAck) {
            delivery.ack();
            return createMessage((Delivery) delivery);
        }
        return new AcknowledgableMessage() {
            @Override
            public void ack() {
                delivery.ack();
            }

            @Override
            public void nack(boolean requeue) {
                delivery.nack(requeue);
            }

            @Override
            public String getExchange() {
                return delivery.getEnvelope().getExchange();
            }

            @Override
            public String getRouteKey() {
                return delivery.getEnvelope().getExchange();
            }

            @Override
            public Map<String, Object> getProperties() {
                return null;
            }

            @Override
            public ByteBuf getPayload() {
                return Unpooled.wrappedBuffer(delivery.getBody());
            }
        };
    }

    protected AmqpMessage createMessage(Delivery delivery) {
        return SimpleAmqpMessage.of(
            delivery.getEnvelope().getExchange(),
            delivery.getEnvelope().getRoutingKey(),
            null,
            Unpooled.wrappedBuffer(delivery.getBody()));
    }

    protected void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        processor = EmitterProcessor.create();
        FluxSink<AmqpMessage> sink = processor.sink(options.getOverflowStrategy());

        Disposable disposable = Flux
            .range(0, consumerThreadSize)
            .map(i -> receiver.consumeManualAck(subscribeQueue, options))
            .as(Flux::merge)
            .doOnNext(msg -> {
                try {
                    sink.next(this.createMessage(msg));
                } catch (Throwable err) {
                    log.error(err.getMessage(), err);
                }
            })
            .subscribe();

        sink.onDispose(() -> {
            running.set(false);
            disposable.dispose();
        });

    }

    @Override
    public Flux<AmqpMessage> subscribe() {
        if (!running.get()) {
            start();
        }
        return processor;
    }

    @Override
    public void shutdown() {
        if (null != processor) {
            processor.onComplete();
        }
        receiver.close();
    }


}
