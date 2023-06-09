package org.jetlinks.pro.simulator.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.core.topic.Topic;
import org.jetlinks.pro.simulator.core.AbstractSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * MQTT会话,在监听器中可以调用{@link this#publish(String, Object, int)},{@link this#publishAsync(String, Object, int)}来推送消息
 *
 * @author zhouhao
 * @since 1.6
 */
@Getter
@Setter
public class MqttSession extends AbstractSession {

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PROTECTED)
    private Mono<MqttClient> connect;

    private MqttClient client;

    private MqttClientOptions options;

    private Topic<FluxSink<MqttMessage>> subscriber = Topic.createRoot();

    public MqttSession(String id, int index) {
        super(id, index);
    }

    @Override
    public String toString() {
        return client == null ? "mqtt connection" : "mqtt(" + client.clientId() + ")";
    }

    public Flux<MqttMessage> subscribe(String topic) {
        return subscribe(topic, 0);
    }

    private String convertTopic(String mqttTopic) {
        return mqttTopic.replace("#", "**").replace("+", "*");
    }

    public Disposable subscribe(String topic, Consumer<MqttMessage> consumer) {
        return subscribe(topic, 0, consumer);
    }

    public Disposable subscribe(String topic, int qos, Consumer<MqttMessage> consumer) {
        return subscribe(topic, qos)
            .subscribe(msg -> {
                try {
                    consumer.accept(msg);
                } catch (Exception e) {
                    error(e);
                }
            });
    }

    public Flux<MqttMessage> subscribe(String topic, int qos) {
        return Flux.create(sink -> {
            Topic<FluxSink<MqttMessage>> sinkTopic = subscriber.append(convertTopic(topic));

            boolean first = sinkTopic.getSubscribers().size() == 0;
            sinkTopic.subscribe(sink);
            sink.onDispose(() -> {
                sinkTopic.unsubscribe(sink);
                if (sinkTopic.getSubscribers().size() == 0) {
                    client.unsubscribe(topic);
                }
            });
            //首次订阅
            if (first) {
                client.subscribe(topic, qos, result -> {
                    if (!result.succeeded()) {
                        sink.error(result.cause());
                    }
                });
            }
        });
    }

    public void publish(String formatPayload) {
        publishAsync(formatPayload).subscribe();
    }

    public void publish(String topic, Object payload) {
        this.publishAsync(topic, payload).subscribe();
    }

    public void publish(String topic, Object payload, int qos) {
        this.publishAsync(topic, payload, qos).subscribe();
    }

    public void publish(MqttMessage mqttMessage) {
        this.publishAsync(mqttMessage).subscribe();
    }

    public Mono<Void> publishAsync(String formatPayload) {
        return publishAsync(SimpleMqttMessage.of(formatPayload));
    }

    public Mono<Void> publishAsync(String topic, Object payload) {
        return this.publishAsync(topic, payload, 0);
    }

    public Mono<Void> publishAsync(String topic, Object payload, int qos) {
        return publishAsync(SimpleMqttMessage
                                .builder()
                                .topic(topic)
                                .payload(encodePayload(payload))
                                .qosLevel(qos)
                                .build());
    }

    public Mono<Void> publishAsync(MqttMessage message) {
        upstream(message);
        return Mono.create(sink -> client.publish(
            message.getTopic(),
            Buffer.buffer(message.getPayload()),
            MqttQoS.valueOf(message.getQosLevel()),
            message.isDup(),
            message.isRetain(),
            result -> {
                if (result.succeeded()) {
                    sink.success();
                } else {
                    sink.error(result.cause());
                }
            }
        ));
    }


    @Override
    public void close() {
        if (client != null) {
            client.disconnect();
        }
    }

    private void init(MqttClient client) {
        this.client = client
            .exceptionHandler(this::error)
            .closeHandler(v -> this.disconnected())
            .publishCompletionHandler(id -> {

            })
            .publishHandler(msg -> {
                MqttMessage mqttMessage = SimpleMqttMessage
                    .builder()
                    .messageId(msg.messageId())
                    .topic(msg.topicName())
                    .payload(msg.payload().getByteBuf())
                    .clientId(options.getClientId())
                    .dup(msg.isDup())
                    .retain(msg.isRetain())
                    .qosLevel(msg.qosLevel().value())
                    .build();
                downstream(mqttMessage);
                subscriber
                    .findTopic(msg.topicName())
                    .flatMapIterable(Topic::getSubscribers)
                    .subscribe(sink -> {
                        try {
                            sink.next(mqttMessage);
                        } catch (Throwable e) {
                            error(e);
                        }
                    });

            });
    }

    @Override
    protected Mono<Void> doConnect() {
        return connect == null ? Mono.empty() : connect
            .doOnNext(this::init)
            .then();
    }
}
