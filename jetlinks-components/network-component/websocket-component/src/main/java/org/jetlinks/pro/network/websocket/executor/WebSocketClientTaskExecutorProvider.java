package org.jetlinks.pro.network.websocket.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.codec.http.websocket.DefaultWebSocketMessage;
import org.jetlinks.core.message.codec.http.websocket.WebSocketMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.PubSubType;
import org.jetlinks.pro.network.websocket.client.WebSocketClient;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebSocket客户端规则节点提供商,提供在规则引擎中使用WebSocket的能力
 *
 * @author zhouhao
 * @since 1.0
 */
@AllArgsConstructor
@Component
public class WebSocketClientTaskExecutorProvider implements TaskExecutorProvider {

    private final NetworkManager networkManager;

    static {
        WebSocketMessageCodec.register();
    }

    @Override
    public String getExecutor() {
        return "websocket-client";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new WebSocketClientTaskExecutor(context));
    }

    class WebSocketClientTaskExecutor extends AbstractTaskExecutor {
        WebSocketClientTaskConfiguration config;

        public WebSocketClientTaskExecutor(ExecutionContext context) {
            super(context);
            reload();
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new WebSocketClientTaskConfiguration());

            config.validate();
        }

        @Override
        public void validate() {
            FastBeanCopier.copy(context.getJob().getConfiguration(), new WebSocketClientTaskConfiguration())
                          .validate();
        }

        @Override
        public String getName() {
            return "Websocket Client";
        }

        @Override
        protected Disposable doStart() {
            Disposable.Composite disposable = Disposables.composite();

            if (config.getType() == PubSubType.consumer) {
                disposable.add(networkManager.<WebSocketClient>getNetwork(DefaultNetworkType.WEB_SOCKET_CLIENT, config.getClientId())
                                   .switchIfEmpty(Mono.fromRunnable(() -> context
                                       .getLogger()
                                       .error("web socket client {} not found", config.getClientId())))
                                   .flatMapMany(WebSocketClient::subscribe)
                                   .doOnNext(msg -> context
                                       .getLogger()
                                       .info("received web socket client message:{}", config
                                           .getSubPayloadType()
                                           .read(msg.getPayload())))
                                   .map(r -> RuleDataCodecs.getCodec(WebSocketMessage.class)
                                                           .map(codec -> codec.encode(r, config.getSubPayloadType()))
                                                           .orElse(r.getPayload()))
                                   .flatMap(data -> context
                                       .getOutput()
                                       .write(Mono.just(RuleData.create(data)))
                                       .flatMap(ignore -> context.fireEvent(RuleConstants.Event.complete, RuleData.create(data))))
                                   .onErrorContinue((err, obj) -> context
                                       .getLogger()
                                       .error("consume web socket message error", err))
                                   .subscribe());
            } else {
                disposable.add(
                    context.getInput()
                           .accept()
                           .flatMap(data -> networkManager
                               .<WebSocketClient>getNetwork(DefaultNetworkType.WEB_SOCKET_CLIENT, config.getClientId())
                               .flatMapMany(client -> RuleDataCodecs
                                   .getCodec(WebSocketMessage.class)
                                   .map(codec -> codec.decode(data, config.getSendPayloadType())
                                                      .cast(WebSocketMessage.class)
                                                      .switchIfEmpty(Mono.fromRunnable(() -> context
                                                          .getLogger()
                                                          .warn("can not decode rule data to web socket message:{}", data))))
                                   .orElseGet(() -> Flux.just(DefaultWebSocketMessage.of(WebSocketMessage.Type.TEXT, config
                                       .getSendPayloadType()
                                       .write(data.getData()))))
                                   .flatMap(client::publish)
                                   .all(r -> r))
                           ).subscribe());
            }
            return disposable;
        }
    }

}
