package org.jetlinks.pro.rule.engine.executor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.utils.ExpressionUtils;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@ConditionalOnBean({
    DeviceSelectorBuilder.class
})
@EditorResource(
    id = "device-message-sender",
    name = "设备消息",
    editor = "rule-engine/editor/function/14-device-message.html",
    helper = "rule-engine/i18n/zh-CN/function/14-device-message.html",
    order = 90
)
public class DeviceMessageSendTaskExecutorProvider implements TaskExecutorProvider {

    private final DeviceRegistry registry;

    private final DeviceSelectorBuilder selectorBuilder;

    @Override
    public String getExecutor() {
        return "device-message-sender";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new DeviceMessageSendTaskExecutor(context));
    }

    class DeviceMessageSendTaskExecutor extends FunctionTaskExecutor {

        private DeviceMessageSendConfig config;

        private Function<Map<String, Object>, Flux<DeviceOperator>> selector;

        public DeviceMessageSendTaskExecutor(ExecutionContext context) {
            super("发送设备消息", context);
            reload();
        }

        protected Flux<DeviceOperator> selectDevice(Map<String, Object> ctx) {
            return selector.apply(ctx);
        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            Map<String, Object> ctx = RuleDataHelper.toContextMap(input);

            Flux<DeviceOperator> readySendDevice =
                "ignoreOffline".equals(config.getStateOperator())
                    ? selectDevice(ctx).filterWhen(DeviceOperator::isOnline)
                    : selectDevice(ctx);

            return readySendDevice
                .switchIfEmpty(context.onError(() -> new DeviceOperationException(ErrorCode.SYSTEM_ERROR, "无可用设备"), input))
                .publishOn(Schedulers.parallel())
                .flatMap(device -> config
                    .doSend(ctx, context, device, input)
                    .onErrorResume(error -> context.onError(error, input)))
                .map(reply -> context.newRuleData(input.newData(reply.toJson())))
                ;
        }

        @Override
        public void validate() {
            if (CollectionUtils.isEmpty(context.getJob().getConfiguration())) {
                throw new IllegalArgumentException("配置不能为空");
            }
            FastBeanCopier.copy(context.getJob().getConfiguration(), new DeviceMessageSendConfig()).validate();
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new DeviceMessageSendConfig());
            config.validate();
            if (StringUtils.hasText(config.getSelector())) {
                selector = selectorBuilder.createSelector(config.getSelector())::select;
            } else if (StringUtils.hasText(config.deviceId)) {
                selector = ctx -> registry.getDevice(config.getDeviceId()).flux();
            } else if (StringUtils.hasText(config.productId)) {
                selector = selectorBuilder.createSelector("product('" + config.getProductId() + "')")::select;
            } else {
                selector = ctx -> registry.getDevice((String) ctx
                    .getOrDefault("deviceId", config.getMessage() == null ? null : config.getMessage().get("deviceId")))
                    .flux();
            }
        }

    }


    @Getter
    @Setter
    public static class DeviceMessageSendConfig {

        //设备ID
        private String deviceId;

        //产品ID
        private String productId;

        //设备选择器表达式
        private String selector;

        //消息来源: pre-node(上游节点),fixed(固定消息)
        private String from;

        private Duration timeout = Duration.ofSeconds(10);

        private Map<String, Object> message;

        private boolean async=false;

        private String waitType = "sync";

        private String stateOperator = "ignoreOffline";

        public Map<String, Object> toMap() {
            Map<String, Object> conf = FastBeanCopier.copy(this, new HashMap<>());
            conf.put("timeout", timeout.toString());
            return conf;
        }

        @SuppressWarnings("all")
        public Flux<DeviceMessageReply> doSend(Map<String, Object> ctx,
                                               ExecutionContext context,
                                               DeviceOperator device,
                                               RuleData input) {
            Map<String, Object> message = new HashMap<>("pre-node".equals(from) ? ctx : this.message);
            message.put("messageId", device.getDeviceId());
            message.put("deviceId", device.getDeviceId());
            message.put("timestamp",System.currentTimeMillis());
            return Mono
                .justOrEmpty(MessageType.convertMessage(message))
                .switchIfEmpty(context.onError(() -> new DeviceOperationException(ErrorCode.UNSUPPORTED_MESSAGE), input))
                .cast(DeviceMessage.class)
                .map(msg -> applyMessageExpression(ctx, msg))
                .doOnNext(msg -> msg
                    .addHeader(Headers.async, async || !"sync".equals(waitType))
                    .addHeader(Headers.sendAndForget, "forget".equals(waitType))
                    .addHeader(Headers.timeout, timeout.toMillis()))
                .flatMapMany(msg -> "forget".equals(waitType)
                    ? device.messageSender().send(msg).then(Mono.empty())
                    : device.messageSender().send(msg)
                );
        }

        private ReadPropertyMessage applyMessageExpression(Map<String, Object> ctx, ReadPropertyMessage message) {
            List<String> properties = message.getProperties();

            if (!CollectionUtils.isEmpty(properties)) {
                message.setProperties(
                    properties.stream().map(prop -> ExpressionUtils.analytical(prop, ctx, "spel")).collect(Collectors.toList())
                );
            }

            return message;
        }

        private WritePropertyMessage applyMessageExpression(Map<String, Object> ctx, WritePropertyMessage message) {
            Map<String, Object> properties = message.getProperties();

            if (!CollectionUtils.isEmpty(properties)) {
                message.setProperties(
                    properties.entrySet()
                        .stream()
                        .map(prop -> Tuples.of(prop.getKey(), ExpressionUtils.analytical(String.valueOf(prop.getValue()), ctx, "spel")))
                        .collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2))
                );
            }

            return message;
        }

        private FunctionInvokeMessage applyMessageExpression(Map<String, Object> ctx, FunctionInvokeMessage message) {
            List<FunctionParameter> inputs = message.getInputs();

            if (!CollectionUtils.isEmpty(inputs)) {
                for (FunctionParameter input : inputs) {
                    String stringVal = String.valueOf(input.getValue());
                    if (stringVal.contains("$")) {
                        input.setValue(ExpressionUtils.analytical(stringVal, ctx, "spel"));
                    }
                }
            }

            return message;
        }

        private DeviceMessage applyMessageExpression(Map<String, Object> ctx, DeviceMessage message) {
            if (message instanceof ReadPropertyMessage) {
                return applyMessageExpression(ctx, ((ReadPropertyMessage) message));
            }
            if (message instanceof WritePropertyMessage) {
                return applyMessageExpression(ctx, ((WritePropertyMessage) message));
            }
            if (message instanceof FunctionInvokeMessage) {
                return applyMessageExpression(ctx, ((FunctionInvokeMessage) message));
            }
            return message;
        }

        public void validate() {
            if ("fixed".equals(from)) {
                MessageType.convertMessage(message).orElseThrow(() -> new IllegalArgumentException("不支持的消息格式"));
            }
        }

    }
}
