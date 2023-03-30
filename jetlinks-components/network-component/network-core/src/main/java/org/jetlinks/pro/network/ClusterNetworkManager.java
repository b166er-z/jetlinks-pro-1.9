package org.jetlinks.pro.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.pro.network.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@ConditionalOnBean(NetworkConfigManager.class)
public class ClusterNetworkManager implements NetworkManager, BeanPostProcessor, CommandLineRunner {

    private final NetworkConfigManager configManager;

    private final EventBus eventBus;

    private final Map<String, Map<String, Network>> store = new ConcurrentHashMap<>();

    private final Map<String, NetworkProvider<Object>> providerSupport = new ConcurrentHashMap<>();

    public ClusterNetworkManager(EventBus eventBus, NetworkConfigManager configManager) {
        this.configManager = configManager;
        this.eventBus = eventBus;
    }

    protected void checkNetwork() {
        Flux.fromIterable(store.values())
            .flatMapIterable(Map::values)
            .filter(i -> !i.isAlive())
            .flatMap(network -> {
                NetworkProvider<Object> provider = providerSupport.get(network.getType().getId());
                if (provider == null || !network.isAutoReload()) {
                    return Mono.empty();
                }
                return configManager.getConfig(network.getType(), network.getId())
                                    .filter(NetworkProperties::isEnabled)
                                    .flatMap(provider::createConfig)
                                    .map(conf -> this.doCreate(provider, network.getId(), conf))
                                    .onErrorContinue((err, res) -> log.warn("reload network [{}] error", network, err));
            })
            .subscribe(net -> log.info("reloaded network :{}", net));
    }

    private Map<String, Network> getNetworkStore(String type) {
        return store.computeIfAbsent(type, _id -> new ConcurrentHashMap<>());
    }

    private Map<String, Network> getNetworkStore(NetworkType type) {
        return getNetworkStore(type.getId());
    }

    private <T extends Network> Mono<T> getNetwork(String type, String id) {
        Map<String, Network> networkMap = getNetworkStore(type);
        return Mono.justOrEmpty(networkMap.get(id))
                   .filter(Network::isAlive)
                   .switchIfEmpty(Mono.defer(() -> createNetwork(SimpleNetworkType.of(type), id)))
                   .map(n -> (T) n);
    }

    @Getter
    @AllArgsConstructor(staticName = "of")
    private static class SimpleNetworkType implements NetworkType {

        private final String id;

        @Override
        public String getName() {
            return getId();
        }
    }

    @Override
    public <T extends Network> Mono<T> getNetwork(NetworkType type, String id) {
        return getNetwork(type.getId(), id);
    }

    @Override
    public Flux<Network> getNetworks() {
        return Flux.fromIterable(store.values())
                   .flatMap(map -> Flux.fromIterable(map.values()));
    }

    public Network doCreate(NetworkProvider<Object> provider, String id, Object properties) {
        return getNetworkStore(provider.getType()).compute(id, (s, network) -> {
            if (network == null) {
                network = provider.createNetwork(properties);
            } else {
                //单例，已经存在则重新加载
                provider.reload(network, properties);
            }
            return network;
        });
    }

    private Mono<Network> createNetwork(NetworkType type, String id) {
        return Mono.justOrEmpty(providerSupport.get(type.getId()))
                   .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的类型:" + type.getName())))
                   .flatMap(provider -> configManager
                       .getConfig(type, id)
                       //.switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("网络[" + type.getName() + "]配置[" + id + "]不存在")))
                       .filter(NetworkProperties::isEnabled)
                      // .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("网络[" + type.getName() + "]配置[" + id + "]已禁用")))
                       .flatMap(provider::createConfig)
                       .map(config -> doCreate(provider, id, config)));
    }

    public void register(NetworkProvider<Object> provider) {
        this.providerSupport.put(provider.getType().getId(), provider);
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        if (bean instanceof NetworkProvider) {
            register(((NetworkProvider) bean));
        }
        return bean;
    }

    @Override
    public List<NetworkProvider<?>> getProviders() {
        return new ArrayList<>(providerSupport.values());
    }
    private Mono<Void> doReload(String type, String id) {
        Optional.ofNullable(getNetworkStore(type).get(id))
                .ifPresent(Network::shutdown);
        return this
            .getNetwork(type, id)
            .then();
    }

    public Mono<Void> doShutdown(String type, String id) {
        return Mono
            .justOrEmpty(getNetworkStore(type).get(id))
            .doOnNext(Network::shutdown)
            .then();
    }

    public Mono<Void> doDestroy(String type, String id) {
        return Mono
            .justOrEmpty(getNetworkStore(type).remove(id))
            .doOnNext(Network::shutdown)
            .then();
    }

    @Override
    public Mono<Void> reload(NetworkType type, String id) {
        return doReload(type.getId(), id)
            .then(
                eventBus.publish("/_sys/network/" + type.getId() + "/reload", id)
            ).then();

    }

    @Override
    public Mono<Void> shutdown(NetworkType type, String id) {
        return this
            .doShutdown(type.getId(), id)
            .then(eventBus.publish("/_sys/network/" + type.getId() + "/shutdown", id))
            .then();
    }

    @Override
    public Mono<Void> destroy(NetworkType type, String id) {
        return this
            .doDestroy(type.getId(), id)
            .then(eventBus.publish("/_sys/network/" + type.getId() + "/destroy", id))
            .then();
    }


    @Override
    public void run(String... args) {
        //定时检查网络组件状态
        Flux.interval(Duration.ofSeconds(10))
            .subscribe(t -> this.checkNetwork());

        eventBus
            .subscribe(Subscription
                           .builder()
                           .subscriberId("network-config-manager")
                           .topics("/_sys/network/*/*")
                           .justBroker()
                           .build())
            .flatMap(payload -> {
                Map<String, String> vars = payload.getTopicVars("/_sys/network/{type}/{action}");
                String id = payload.bodyToString(true);
                log.debug("{} network [{}-{}]", vars.get("action"), vars.get("type"), id);
                if ("reload".equals(vars.get("action"))) {
                    return this
                        .doReload(vars.get("type"), id)
                        .onErrorResume(err -> {
                            log.error("reload network error", err);
                            return Mono.empty();
                        });
                }
                if ("shutdown".equals(vars.get("action"))) {
                    return doShutdown(vars.get("type"), id);
                }
                if ("destroy".equals(vars.get("action"))) {
                    return doDestroy(vars.get("type"), id);
                }
                return Mono.empty();
            })
            .subscribe();
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Synchronization implements Serializable {
        private NetworkType type;

        private String id;
    }
}
