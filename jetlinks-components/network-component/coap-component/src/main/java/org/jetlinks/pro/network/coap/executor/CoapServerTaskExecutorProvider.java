package org.jetlinks.pro.network.coap.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.coap.server.CoapServer;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * CoAP服务端规则任务执行器,用于执行在规则引擎中定义的CoAP客户端节点
 *
 * @author zhouhao
 * @since 1.0
 */
@AllArgsConstructor
public class CoapServerTaskExecutorProvider implements TaskExecutorProvider {

    private final NetworkManager clientManager;

    @Override
    public String getExecutor() {
        return "coap-server";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new CoapServerTaskExecutor(context));
    }

    class CoapServerTaskExecutor extends AbstractTaskExecutor {

        CoapServerTaskConfiguration config;

        public CoapServerTaskExecutor(ExecutionContext context) {
            super(context);
            reload();
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new CoapServerTaskConfiguration());
        }

        @Override
        public String getName() {
            return "CoAP Server";
        }

        @Override
        protected Disposable doStart() {

            return clientManager
                .<CoapServer>getNetwork(DefaultNetworkType.COAP_SERVER, config.getServerId())
                .switchIfEmpty(Mono.fromRunnable(() -> context
                    .getLogger()
                    .error("CoAP服务[{}]不存在", config.getServerId())))
                .flatMapMany(CoapServer::subscribe)
                .flatMap(coapExchange -> config
                    .doRespond(coapExchange, null)
                    .onErrorResume(err -> context.onError(err, null))
                )
                .subscribe();
        }
    }
}
