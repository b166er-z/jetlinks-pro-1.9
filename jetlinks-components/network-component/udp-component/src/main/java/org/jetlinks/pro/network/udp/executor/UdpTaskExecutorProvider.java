package org.jetlinks.pro.network.udp.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.PubSubType;
import org.jetlinks.pro.network.udp.UdpMessage;
import org.jetlinks.pro.network.udp.UdpSupport;
import org.jetlinks.pro.network.udp.codec.UdpMessageCodec;
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
 * UDP 规则节点执行器提供商，用于提供在规则引擎中使用UDP的支持
 *
 * @author zhouhao
 * @since 1.6
 */
@AllArgsConstructor
@Component
public class UdpTaskExecutorProvider implements TaskExecutorProvider {

    private final NetworkManager networkManager;

    static {
        UdpMessageCodec.register();
    }

    @Override
    public String getExecutor() {
        return "udp";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new UdpTaskExecutor(context));
    }

    class UdpTaskExecutor extends AbstractTaskExecutor {

        private UdpNodeConfiguration config;

        public UdpTaskExecutor(ExecutionContext context) {
            super(context);
            reload();
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new UdpNodeConfiguration());
            config.validate();
        }

        @Override
        public void validate() {
            FastBeanCopier
                .copy(context.getJob().getConfiguration(), new UdpNodeConfiguration())
                .validate();
        }

        @Override
        public String getName() {
            return "UDP";
        }

        @Override
        protected Disposable doStart() {

            Disposable.Composite disposable = Disposables.composite();


            if (config.getType() == PubSubType.producer) {
                disposable.add(
                    context.getInput()
                           .accept()
                           .flatMap(data -> networkManager.<UdpSupport>getNetwork(DefaultNetworkType.UDP, config.getClientId())
                               .flatMapMany(client -> RuleDataCodecs
                                   .getCodec(UdpMessage.class)
                                   .map(codec -> codec.decode(data, config.getSendPayloadType())
                                                      .cast(UdpMessage.class)
                                                      .switchIfEmpty(Mono.fromRunnable(() -> context
                                                          .getLogger()
                                                          .warn("can not decode rule data to udp message:{}", data))))
                                   .orElseGet(() -> Flux.just(new UdpMessage(config
                                                                                 .getSendPayloadType()
                                                                                 .write(data.getData()))))
                                   .flatMap(client::publish)
                                   .then(Mono.just(data)))).subscribe()
                );
            }
            if (config.getType() == PubSubType.consumer) {

                disposable.add(networkManager.<UdpSupport>getNetwork(DefaultNetworkType.UDP, config.getClientId())
                                   .switchIfEmpty(Mono.fromRunnable(() -> context
                                       .getLogger()
                                       .error("udp client {} not found", config.getClientId())))
                                   .flatMapMany(UdpSupport::subscribe)
                                   .doOnNext(msg -> context
                                       .getLogger()
                                       .info("received udp client message:{}", config
                                           .getSubPayloadType()
                                           .read(msg.getPayload())))
                                   .map(r -> RuleDataCodecs.getCodec(UdpMessage.class)
                                                           .map(codec -> codec.encode(r, config.getSubPayloadType()))
                                                           .orElse(r.getPayload()))
                                   .flatMap(data -> context.getOutput()
                                                           .write(Mono.just(RuleData.create(data)))
                                                           .flatMap(r -> context.fireEvent(RuleConstants.Event.result, RuleData
                                                               .create(data)))
                                   )
                                   .onErrorContinue((err, obj) -> {
                                       context.getLogger().error("consume udp message error", err);
                                   })
                                   .subscribe());
            }

            return disposable;
        }
    }

}
