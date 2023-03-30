package org.jetlinks.pro.network.coap.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.coap.client.CoapClient;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.LambdaTaskExecutor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * CoAP客户端规则任务执行器,用于执行在规则引擎中定义的CoAP客户端节点
 *
 * @author zhouhao
 * @since 1.0
 */
@AllArgsConstructor
public class CoapClientTaskExecutorProvider implements TaskExecutorProvider {


    private final NetworkManager clientManager;


    public Function<RuleData, Publisher<?>> createExecutor(ExecutionContext context, CoapClientTaskConfiguration config) {
        return data -> clientManager
            .<CoapClient>getNetwork(DefaultNetworkType.COAP_CLIENT, config.getClientId())
            .switchIfEmpty(Mono.fromRunnable(() -> context.getLogger().error("CoAP客户端[{}]不存在", config.getClientId())))
            .flatMapMany(client -> config.doSend(client, data));
    }

    @Override
    public String getExecutor() {
        return "coap-client";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {


        return Mono.just(new LambdaTaskExecutor("CoAP Client", context, () -> {
            CoapClientTaskConfiguration config = FastBeanCopier
                .copy(context.getJob().getConfiguration(), CoapClientTaskConfiguration.class);
            config.validate();
            return createExecutor(context, config);
        }));
    }
}
