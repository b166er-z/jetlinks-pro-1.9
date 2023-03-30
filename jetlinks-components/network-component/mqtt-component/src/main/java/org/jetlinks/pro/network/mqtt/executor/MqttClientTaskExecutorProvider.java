package org.jetlinks.pro.network.mqtt.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.PubSubType;
import org.jetlinks.pro.network.mqtt.client.MqttClient;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResources;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.Task;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MQTT 客户端规则引擎节点提供商
 *
 * @author zhouhao
 * @since 1.0
 */
@AllArgsConstructor
@Component
@EditorResources(
    resources = {
        @EditorResource(
            id = "mqtt in",
            name = "订阅MQTT",
            editor = "rule-engine/editor/network/mqtt-in.html",
            helper = "rule-engine/i18n/zh-CN/network/mqtt-in.html",
            order = 110
        ),
        @EditorResource(
            id = "mqtt out",
            name = "推送MQTT",
            editor = "rule-engine/editor/network/mqtt-out.html",
            helper = "rule-engine/i18n/zh-CN/network/mqtt-out.html",
            order = 120
        )
    }

)
public class MqttClientTaskExecutorProvider implements TaskExecutorProvider {

    private final NetworkManager networkManager;

    static {
        MqttRuleDataCodec.load();
    }

    @Override
    public String getExecutor() {
        return "mqtt-client";
    }

    protected Flux<MqttMessage> convertMessage(RuleData message, MqttClientTaskConfiguration config) {

        return RuleDataCodecs
            .getCodec(MqttMessage.class)
            .map(codec -> codec
                .decode(message,
                        config.getPayloadType(),
                        new MqttTopics(config.getTopics(RuleDataHelper.toContextMap(message))))
                .cast(MqttMessage.class))
            .orElseThrow(() -> new UnsupportedOperationException("unsupported decode message:" + message));
    }

    protected Mono<RuleData> convertMessage(ExecutionContext context, MqttMessage message, MqttClientTaskConfiguration config) {

        return Mono
            .just(RuleDataCodecs
                      .getCodec(MqttMessage.class)
                      .map(codec -> codec.encode(message, config.getPayloadType(), new TopicVariables(config.getTopicVariables())))
                      .map(context::newRuleData)
                      .orElseGet(() -> context.newRuleData(message)));
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new MqttClientTaskExecutor(context));
    }

    class MqttClientTaskExecutor extends AbstractTaskExecutor {

        private MqttClientTaskConfiguration config;

        public MqttClientTaskExecutor(ExecutionContext context) {
            super(context);
            reload();
        }

        @Override
        public String getName() {
            return "MQTT Client";
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new MqttClientTaskConfiguration());
            config.validate();
            if (disposable != null) {
                disposable.dispose();
            }
        }

        @Override
        public void validate() {
            FastBeanCopier
                .copy(context.getJob().getConfiguration(), new MqttClientTaskConfiguration())
                .validate();
        }

        @Override
        protected Disposable doStart() {
            Disposable.Composite disposable = Disposables.composite();

            if (EnumDict.in(PubSubType.producer, config.getClientType())) {
                disposable.add(context
                                   .getInput()
                                   .accept()
                                   .filter((data) -> state == Task.State.running)
                                   .flatMap(data ->
                                                networkManager
                                                    .<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, config.getClientId())
                                                    .flatMapMany(client -> convertMessage(data, config)
                                                        .flatMap(msg -> client
                                                            .publish(msg)
                                                            .doOnSuccess((v) -> context
                                                                .getLogger()
                                                                .debug("推送MQTT[{}]消息:{}", client.getId(), msg))

                                                        )
                                                    ).onErrorResume((err) -> context.onError(err, data))
                                   )
                                   .onErrorResume((err) -> context.onError(err, null))
                                   .subscribe()
                );
            }
            if (EnumDict.in(PubSubType.consumer, config.getClientType())) {
                disposable.add(networkManager
                                   .<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, config.getClientId())
                                   .flatMapMany(client -> client.subscribe(config.getTopics()))
                                   .filter((data) -> state == Task.State.running)
                                   .doOnNext(message -> context.getLogger().debug("收到MQTT消息:{}", message))
                                   .flatMap(message -> convertMessage(context, message, config))
                                   .flatMap(ruleData -> context
                                       .getOutput()
                                       .write(Mono.just(ruleData))
                                       .thenReturn(ruleData))
                                   .flatMap(ruleData -> context
                                       .fireEvent(RuleConstants.Event.result, ruleData)
                                       .thenReturn(ruleData))
                                   .onErrorResume((err) -> context.onError(err, null))
                                   .subscribe());
            }
            return disposable;
        }
    }
}
