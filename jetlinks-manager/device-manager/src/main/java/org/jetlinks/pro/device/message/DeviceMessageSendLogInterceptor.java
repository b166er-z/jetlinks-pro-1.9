package org.jetlinks.pro.device.message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.ValidateResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 发送设备指令的时候,将消息推送到网关中.
 *
 * @author zhouhao
 * @since 1.1
 */
@Component
@Slf4j
@AllArgsConstructor
public class DeviceMessageSendLogInterceptor implements DeviceMessageSenderInterceptor {

    private final EventBus eventBus;

    private final DeviceRegistry registry;

    public Mono<Void> doPublish(Message message) {
        return DeviceMessageConnector
            .createDeviceMessageTopic(registry, message)
            .flatMap(topic -> {
                Mono<Void> publisher = eventBus.publish(topic, message).then();
                if (message instanceof ChildDeviceMessage) {
                    publisher = publisher.then(doPublish(((ChildDeviceMessage) message).getChildDeviceMessage()));
                }
                return publisher;
            })
            .then();
    }

    private Mono<DeviceMessage> convertParameterType(DeviceOperator device, FunctionInvokeMessage message) {
        if (message.getHeader(Headers.force).orElse(false)) {
            return Mono.just(message);
        }
        return device
            .getMetadata()
            .doOnNext(metadata -> {
                FunctionMetadata function = metadata
                    .getFunction(message.getFunctionId())
                    .orElseThrow(() -> new DeviceOperationException(ErrorCode.FUNCTION_UNDEFINED, "功能[" + message
                        .getFunctionId() + "]未定义"));
                Map<String, FunctionParameter> parameters = message
                    .getInputs()
                    .stream()
                    .collect(Collectors.toMap(FunctionParameter::getName, Function.identity()));
               message.addHeaderIfAbsent(Headers.async, function.isAsync());
                for (PropertyMetadata input : function.getInputs()) {
                    FunctionParameter parameter = parameters.get(input.getId());
                    if (parameter == null) {
                        continue;
                    }
                    ValidateResult result = input.getValueType().validate(parameter.getValue());
                    parameter.setValue(result.assertSuccess());
                }
            })
            .thenReturn(message);
    }

    private Mono<DeviceMessage> prepareMessage(DeviceOperator device, DeviceMessage message) {
        if (message instanceof FunctionInvokeMessage) {
            return convertParameterType(device, ((FunctionInvokeMessage) message));
        }
        return Mono.just(message);
    }

    @Override
    public Mono<DeviceMessage> preSend(DeviceOperator device, DeviceMessage message) {
        if (message instanceof RepayableDeviceMessage) {
            return this
                .prepareMessage(device, message)
                .flatMap(msg -> this
                    .doPublish(msg)
                    .thenReturn(msg)
                    .doOnEach(ReactiveLogger.onComplete(() -> {
                        if (log.isDebugEnabled()) {
                            log.debug("向设备[{}]发送指令:{}", msg.getDeviceId(), msg.toString());
                        }
                    })));
        } else {
            return Mono.just(message);
        }

    }
}
