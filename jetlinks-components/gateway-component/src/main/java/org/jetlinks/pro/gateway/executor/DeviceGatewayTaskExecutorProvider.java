package org.jetlinks.pro.gateway.executor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.pro.gateway.external.Message;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.Optional;

@Component
@AllArgsConstructor
@ConditionalOnBean({
    MessageHandler.class,DecodedClientMessageHandler.class,
    DeviceRegistry.class,DeviceSessionManager.class
})
public class DeviceGatewayTaskExecutorProvider implements TaskExecutorProvider {

    private final MessageHandler messageHandler;

    private final DecodedClientMessageHandler clientMessageHandler;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    @Override
    public String getExecutor() {
        return "device-gateway";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new DeviceGatewayTaskExecutor(context));
    }

    class DeviceGatewayTaskExecutor extends AbstractTaskExecutor {

        public DeviceGatewayTaskExecutor(ExecutionContext context) {
            super(context);
        }

        @Override
        public String getName() {
            return "设备网关";
        }

        @Override
        protected Disposable doStart() {
            Disposable.Composite disposable = Disposables.composite();
            disposable.add(messageHandler
                .handleSendToDeviceMessage(context.getInstanceId() + ":" + context.getJob().getNodeId())
                .flatMap(msg ->
                    context
                        .getOutput()
                        .write(Mono.just(context.newRuleData(msg))))
                .subscribe());

            disposable.add(
                context
                    .getInput()
                    .accept()
                    .flatMap(ruleData -> {
                        Mono<Void> handle;
                        if (ruleData.getData() instanceof DeviceMessage) {
                            handle = handleMessage(ruleData, ((DeviceMessage) ruleData.getData()));
                        } else {
                            handle = ruleData.dataToMap()
                                .map(map -> MessageType.convertMessage(map).orElseThrow(() -> new UnsupportedOperationException("不支持的消息:" + map)))
                                .cast(DeviceMessage.class)
                                .flatMap(msg -> handleMessage(ruleData, msg))
                                .then();
                        }
                        return handle
                            .then(context.fireEvent(RuleConstants.Event.complete, ruleData))
                            .onErrorResume(err -> context.onError(err, ruleData));
                    })
                    .subscribe()
            );


            return disposable;
        }

        protected Mono<Void> handleMessage(RuleData input, DeviceMessage deviceMessage) {
            if (StringUtils.isEmpty(deviceMessage.getDeviceId())) {
                context.getLogger().warn("deviceId is null or empty:{}", deviceMessage);
                return Mono.empty();
            }

            return registry
                .getDevice(deviceMessage.getDeviceId())
                .flatMap(operator->{
                    DeviceSession session = sessionManager.getSession(operator.getDeviceId());


                   return clientMessageHandler.handleMessage(operator,deviceMessage);

                }).then();
        }
    }

}
