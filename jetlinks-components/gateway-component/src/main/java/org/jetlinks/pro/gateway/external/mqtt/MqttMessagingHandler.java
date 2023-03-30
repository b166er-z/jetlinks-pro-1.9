package org.jetlinks.pro.gateway.external.mqtt;

import com.alibaba.fastjson.JSON;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.jetlinks.pro.gateway.external.Message;
import org.jetlinks.pro.gateway.external.MessagingManager;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class MqttMessagingHandler {

    private final Vertx vertx;

    private final MqttServerOptions options;

    private final MessagingManager messagingManager;

    private final MqttAuthenticationHandler authenticationHandler;

    public void startup() {

        MqttServer mqttServer = MqttServer.create(vertx, options);
        init(mqttServer);
        mqttServer.listen(mqtt -> {
            if (mqtt.succeeded()) {
                log.debug("startup mqtt messaging server on [{}:{}]", options.getHost(), options.getPort());
            } else {
                log.error("cannot startup mqtt messaging server: [{}:{}]", options.getHost(), options.getPort(), mqtt.cause());
            }
        });
    }

    public void init(MqttServer mqttServer) {

        Flux
            .<MqttEndpoint>create(sink -> mqttServer
                .endpointHandler(sink::next)
                .exceptionHandler(err -> log.error(err.getMessage(), err)))
            .flatMap(endpoint ->
                    authenticationHandler
                        .handle(endpoint.clientIdentifier(), endpoint.auth())
                        .switchIfEmpty(Mono.fromRunnable(() -> endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD)))
                        .doOnNext(authentication -> acceptConnection(authentication, endpoint))
                        .onErrorResume((err) -> {
                            try {
                                endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                            }catch (Throwable ignore){

                            }
                            log.error(err.getMessage(), err);
                            return Mono.empty();
                        })
                , Integer.MAX_VALUE
            )
            .onErrorResume((err) -> {
                log.error(err.getMessage(), err);
                return Mono.empty();
            })
            .subscribe();

    }

    private String getTopicUrl(String topic) {
        topic = (topic.contains("?") && topic.contains("=")) ? topic.substring(0, topic.lastIndexOf("?")) : topic;
        if (topic.endsWith("/")) {
            return topic.substring(0, topic.length() - 1);
        }
        return topic;
    }

    private Flux<Message> doSubscribe(String topic, Authentication authentication) {
        boolean shared = false;
        if (topic.startsWith("$shared/")) {
            shared = true;
            topic = topic.substring(7);
        }
        String topicString = getTopicUrl(topic);
        Map<String, Object> parameter;
        if ((topic.contains("?") && topic.contains("="))) {
            parameter = new HashMap<>(HttpUtils.parseEncodedUrlParams(topic.substring(topic.lastIndexOf("?") + 1)));
        } else {
            parameter = new HashMap<>();
        }

        topicString = topicString.replace("#", "**").replace("+", "*");
        SubscribeRequest request = new SubscribeRequest();
        request.setId(DigestUtils.md5Hex(topicString));
        request.setShared(shared);
        request.setAuthentication(authentication);
        request.setParameter(parameter);
        request.setTopic(topicString);
        return messagingManager.subscribe(request);
    }

    private void acceptConnection(Authentication authentication, MqttEndpoint endpoint) {
        Map<String, Disposable> subs = new ConcurrentHashMap<>();

        Runnable close = () -> {
            subs.values().forEach(Disposable::dispose);
            subs.clear();
        };
        endpoint
            .publishHandler(msg -> {
                //not support publish message
            })
            .disconnectHandler(nil -> close.run())
            .unsubscribeHandler(sub -> {
                for (String topic : sub.topics()) {
                    Optional.ofNullable(subs.remove(topic))
                        .ifPresent(Disposable::dispose);
                }
            })
            .subscribeHandler(message -> {
                List<String> topics = message.topicSubscriptions().stream()
                    .map(MqttTopicSubscription::topicName)
                    .collect(Collectors.toList());

                endpoint
                    .subscribeAcknowledge(message.messageId(), message.topicSubscriptions().stream()
                        .map(MqttTopicSubscription::qualityOfService).collect(Collectors.toList()));

                Disposable disposable = Flux
                    .fromIterable(message.topicSubscriptions())
                    .flatMap(topic ->
                        {

                            String topicName = topic.topicName();
                            String prefix = topicName.startsWith("$shared/") ? "$shared" : "";
                            return this
                                .doSubscribe(topicName, authentication)
                                .doOnNext(msg -> {
                                    try {
                                        if (!endpoint.isConnected()) {
                                            close.run();
                                            return;
                                        }
                                        String topicString = msg.getTopic().replace("**", "#").replace("*", "+");
                                        endpoint.publish(
                                            prefix + topicString,
                                            Buffer.buffer(JSON.toJSONString(msg.getPayload())),
                                            topic.qualityOfService(),
                                            false,
                                            false
                                        );
                                    } catch (Throwable e) {
                                        log.error(e.getMessage(), e);
                                    }
                                });

                        }
                        , Integer.MAX_VALUE)
                    .subscribe();
                for (String topic : topics) {
                    subs.put(topic, disposable);
                }

            })
            //QoS 1 PUBACK
            .publishAcknowledgeHandler(messageId -> {

            })
            //QoS 2  PUBREC
            .publishReceivedHandler(endpoint::publishRelease)
            //QoS 2  PUBREL
            .publishReleaseHandler(endpoint::publishComplete)
            //QoS 2  PUBCOMP
            .publishCompletionHandler(messageId -> {

            })
            .accept();
    }

}
