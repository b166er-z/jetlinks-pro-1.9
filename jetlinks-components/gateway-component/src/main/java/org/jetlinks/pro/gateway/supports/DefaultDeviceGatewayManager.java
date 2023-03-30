package org.jetlinks.pro.gateway.supports;

import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.gateway.DeviceGatewayManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnBean(DeviceGatewayPropertiesManager.class)
@Slf4j
public class DefaultDeviceGatewayManager implements DeviceGatewayManager, BeanPostProcessor {

    private final DeviceGatewayPropertiesManager propertiesManager;

    private final Map<String, DeviceGatewayProvider> providers = new ConcurrentHashMap<>();

    private final Map<String, DeviceGateway> store = new ConcurrentHashMap<>();

    private final EventBus eventBus;

    public DefaultDeviceGatewayManager(DeviceGatewayPropertiesManager propertiesManager, EventBus eventBus) {
        this.propertiesManager = propertiesManager;
        this.eventBus = eventBus;

        this.eventBus.subscribe(Subscription
                                    .builder()
                                    .subscriberId("device-gateway-state-listener")
                                    .justBroker()
                                    .topics("/_sys/device-gateway/*/*")
                                    .build())
                     .subscribe(msg -> {
                         try {
                             Map<String, String> var = msg.getTopicVars("/_sys/device-gateway/{id}/{state}");
                             if ("start".equals(var.get("state"))) {
                                 doStart(var.get("id"))
                                     .subscribe();
                             } else {
                                 doShutdown(var.get("id"))
                                     .subscribe();
                             }
                         }catch (Exception e){
                             log.error(e.getMessage(),e);
                         }finally {
                             ReferenceCountUtil.safeRelease(msg);
                         }
                     })
        ;
    }

    private Mono<DeviceGateway> doGetGateway(String id) {
        if (store.containsKey(id)) {
            return Mono.just(store.get(id));
        }
        return propertiesManager
            .getProperties(id)
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("网关配置[" + id + "]不存在")))
            .flatMap(properties -> Mono
                .justOrEmpty(providers.get(properties.getProvider()))
                .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的网关类型[" + properties.getProvider() + "]")))
                .flatMap(provider -> provider
                    .createDeviceGateway(properties)
                    .flatMap(gateway -> {
                        if (store.containsKey(id)) {
                            return gateway
                                .shutdown()
                                .thenReturn(store.get(id));
                        }
                        store.put(id, gateway);
                        return Mono.justOrEmpty(gateway);
                    })));
    }

    public Mono<Void> doShutdown(String gatewayId) {
        return Mono.justOrEmpty(store.remove(gatewayId))
                   .flatMap(DeviceGateway::shutdown)
                   .doOnSuccess(nil-> log.debug("shutdown device gateway {}", gatewayId))
                   .doOnError(err->log.error("shutdown device gateway {} error",gatewayId,err));
    }

    @Override
    public Mono<Void> shutdown(String gatewayId) {
        return doShutdown(gatewayId)
            .then(eventBus.publish("/_sys/device-gateway/" + gatewayId + "/shutdown", gatewayId).then());
    }

    public Mono<Void> doStart(String id) {
        return this
            .getGateway(id)
            .flatMap(DeviceGateway::startup)
            .doOnSuccess(nil-> log.debug("started device gateway {}", id))
            .doOnError(err->log.error("start device gateway {} error",id,err));
    }

    @Override
    public Mono<Void> start(String id) {
        return this
            .doStart(id)
            .then(eventBus.publish("/_sys/device-gateway/" + id + "/start", id).then());
    }

    @Override
    public Mono<DeviceGateway> getGateway(String id) {
        return Mono
            .justOrEmpty(store.get(id))
            .switchIfEmpty(doGetGateway(id));
    }

    @Override
    public List<DeviceGatewayProvider> getProviders() {
        return new ArrayList<>(providers.values());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DeviceGatewayProvider) {
            DeviceGatewayProvider provider = ((DeviceGatewayProvider) bean);
            providers.put(provider.getId(), provider);
        }
        return bean;
    }


}
