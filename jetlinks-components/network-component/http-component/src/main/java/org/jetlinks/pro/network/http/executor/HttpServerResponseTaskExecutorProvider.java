package org.jetlinks.pro.network.http.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.codec.http.SimpleHttpResponseMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.http.device.HttpServerExchangeMessage;
import org.jetlinks.pro.network.http.server.HttpServer;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.Task;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * HTTP 服务监听响应规则节点提供商
 *
 * @author zhouhao
 * @since 1.3
 */
@Component
@AllArgsConstructor
@EditorResource(
    id = "http response",
    name = "响应HTTP",
    editor = "rule-engine/editor/network/22-httpresponse.html",
    helper = "rule-engine/i18n/zh-CN/network/22-httpresponse.html",
    order = 140
)
public class HttpServerResponseTaskExecutorProvider implements TaskExecutorProvider {

    private final HttpServerTaskExecutorManager executorManager;

    @Override
    public String getExecutor() {
        return "http-response";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new HttpServerTaskExecutor(context));
    }

    class HttpServerTaskExecutor extends FunctionTaskExecutor {

        private HttpServerResponseConfig listenerConfig;

        public HttpServerTaskExecutor(ExecutionContext context) {
            super("Http Response", context);
            reload();
        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            //直接响应
            return executorManager
                .doResponse(listenerConfig, input)
                .then(Mono.empty())
                ;
        }

        @Override
        public String getName() {
            return "Http Server Response";
        }

        @Override
        public void reload() {
            listenerConfig = FastBeanCopier.copy(context.getJob().getConfiguration(), new HttpServerResponseConfig());
            listenerConfig.validate();
        }

        @Override
        public void validate() {
            FastBeanCopier.copy(context.getJob().getConfiguration(), new HttpServerResponseConfig())
                          .validate();
        }


    }
}
