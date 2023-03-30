package org.jetlinks.pro.network.http.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.http.server.HttpServer;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.Task;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * HTTP服务监听规则节点提供商,用于支持监听HTTP请求,并传入到下游规则节点进行处理。
 * 处理完成后，需要连接到HTTP响应{@link HttpServerResponseTaskExecutorProvider}节点
 *
 * @author zhouhao
 * @since 1.3
 * @see HttpServerResponseTaskExecutorProvider
 */
@Component
@AllArgsConstructor
@EditorResource(
    id = "http in",
    name = "监听HTTP",
    editor = "rule-engine/editor/network/21-httpin.html",
    helper = "rule-engine/i18n/zh-CN/network/21-httpin.html",
    order = 130
)
public class HttpServerListenerTaskExecutorProvider implements TaskExecutorProvider {

    private final NetworkManager networkManager;

    private final HttpServerTaskExecutorManager executorManager;

    @Override
    public String getExecutor() {
        return "http-listener";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new HttpServerTaskExecutor(context));
    }

    class HttpServerTaskExecutor extends AbstractTaskExecutor {

        private HttpServerListenerConfig listenerConfig;

        public HttpServerTaskExecutor(ExecutionContext context) {
            super(context);
            reload();
        }

        @Override
        public String getName() {
            return "Http Server";
        }

        @Override
        public void reload() {
            listenerConfig = FastBeanCopier.copy(context.getJob().getConfiguration(), new HttpServerListenerConfig());
            listenerConfig.validate();
        }

        @Override
        public void validate() {
            FastBeanCopier.copy(context.getJob().getConfiguration(), new HttpServerListenerConfig())
                          .validate();
        }

        @Override
        protected Disposable doStart() {
            return networkManager
                .<HttpServer>getNetwork(DefaultNetworkType.HTTP_SERVER, listenerConfig.getServerId())
                .flatMap(httpServer -> httpServer
                    .handleRequest(listenerConfig.getMethod().name().toLowerCase(), listenerConfig.getUrl())
                    .filter(exchange -> state == Task.State.running)
                    .flatMap(exchange -> exchange
                        .toExchangeMessage()
                        .map(msg -> executorManager.addWaitResponse(listenerConfig.getUrl(), context.newRuleData(null), msg))
                        .flatMap(data -> context
                            .getOutput()
                            .write(Mono.just(data))
                            .then(context.fireEvent(RuleConstants.Event.result, data))
                        )
                        .onErrorResume(err -> context
                            .onError(err, null)
                            .then(exchange.error(HttpStatus.INTERNAL_SERVER_ERROR, err))
                        )
                        .onErrorResume(err -> context.onError(err, null))
                    )
                    .then()
                )
                .onErrorResume(err -> context.onError(err, null))
                .subscribe();
        }
    }
}
