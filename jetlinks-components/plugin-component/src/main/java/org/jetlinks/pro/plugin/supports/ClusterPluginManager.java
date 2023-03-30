package org.jetlinks.pro.plugin.supports;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.pro.plugin.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ClusterPluginManager implements
    PluginManager,
    BeanPostProcessor {

    private final PluginProviderLoader providerLoader;

    private final ClusterManager clusterManager;

    private final ApplicationContext applicationContext;

    private final CompositePluginListener pluginListener = new CompositePluginListener();

    private final Map<String, ClusterPlugin> pluginStore = new ConcurrentHashMap<>();

    private final Map<String, PluginProvider> providers = new ConcurrentHashMap<>();

    public ClusterPluginManager(PluginProviderLoader providerLoader,
                                ClusterManager clusterManager,
                                ApplicationContext applicationContext) {
        this.providerLoader = providerLoader;
        this.clusterManager = clusterManager;
        this.applicationContext = applicationContext;
    }

    @Override
    public Mono<Plugin> getPlugin(String id) {
        return Mono.justOrEmpty(pluginStore.get(id));
    }

    @Override
    public Flux<Plugin> getPlugins() {
        return Flux.fromIterable(pluginStore.values());
    }

    @Override
    public Mono<PluginProvider> getProvider(String provider) {
        return Mono.justOrEmpty(providers.get(provider));
    }

    @Override
    public Flux<PluginProvider> getProviders() {
        return Flux.fromIterable(providers.values());
    }

    @Override
    public Flux<Plugin> install(PluginPackage pluginPackage) {
        return load(pluginPackage)
            .collectList()
            .flatMapMany(list ->
                Flux.fromIterable(clusterManager.getHaManager().getAllNode())
                    //给集群的每个节点都发送安装通知
                    .flatMap(serverNode -> sendNotify("/plugin/install", pluginPackage))
                    //如果发生错误则卸载插件
                    .onErrorResume(err -> Flux.fromIterable(list)
                        .flatMap(plugin -> uninstall(plugin.getId()))
                        .then(Mono.error(err)))
                    .thenMany(Flux.fromIterable(list))
            );
    }

    protected Mono<Void> sendNotify(String address, Object message) {
        return Flux.fromIterable(clusterManager.getHaManager().getAllNode())
            .flatMap(serverNode -> this.clusterManager
                .getNotifier()
                .sendNotifyAndReceive(serverNode.getId(), address, Mono.just(message)))
            .then();
    }

    @Override
    @SuppressWarnings("all")
    public Flux<Plugin> load(PluginPackage pluginPackage) {
        return providerLoader.loadProvider(pluginPackage)
            .doOnNext(provider -> providers.put(provider.getId(), provider))
            .flatMap(provider -> doLoad(pluginPackage, provider))
            .flatMap(plugin -> pluginListener.onInstall(plugin))
            ;
    }

    @PostConstruct
    public void startup() {
        clusterManager
            .getNotifier()
            .<String, Boolean>handleNotify("/plugin/uninstall", id -> {
                this.doUnInstall(id);
                return Mono.just(true);
            })
            .onErrorResume(err -> {
                log.error(err.getMessage(), err);
                return Mono.empty();
            })
            .subscribe();

        clusterManager
            .getNotifier()
            .<PluginPackage, Boolean>handleNotify("/plugin/install", request -> load(request).then(Mono.just(true)))
            .onErrorResume(err -> {
                log.error(err.getMessage(), err);
                return Mono.empty();
            })
            .subscribe();

        clusterManager
            .getNotifier()
            .<String, Boolean>handleNotify("/plugin/start", request -> Mono
                .justOrEmpty(pluginStore.get(request))
                .flatMap(plugin -> plugin.
                    getTarget()
                    .start()
                    .thenReturn(true)))
            .onErrorResume(err -> {
                log.error(err.getMessage(), err);
                return Mono.empty();
            })
            .subscribe();

        clusterManager
            .getNotifier()
            .<String, Boolean>handleNotify("/plugin/stop", request -> Mono
                .justOrEmpty(pluginStore.get(request))
                .flatMap(plugin -> plugin.
                    getTarget()
                    .stop()
                    .thenReturn(true)))
            .onErrorResume(err -> {
                log.error(err.getMessage(), err);
                return Mono.empty();
            })
            .subscribe();

    }

    @SuppressWarnings("all")
    protected Mono<? extends Plugin> doLoad(PluginPackage pluginPackage,
                                            PluginProvider provider) {

        return provider.createPlugin(pluginPackage, new DefaultPluginRunnerContext())
            .map(plugin -> new ClusterPlugin(pluginPackage.getId(), plugin))
            .flatMap(plugin -> {
                ClusterPlugin old = pluginStore.put(plugin.getId(), plugin);
                if (null != old) {
                    return old.getTarget()
                        .stop()
                        .thenReturn(plugin);
                }
                return Mono.just(plugin);
            });
    }

    protected void doUnInstall(String id) {
        log.debug("uninstall plugin [{}]", id);
        Optional.ofNullable(pluginStore.remove(id))
            .ifPresent(Plugin::destroy);
    }

    @Override
    public Mono<Void> uninstall(String id) {
        doUnInstall(id);
        return Flux.fromIterable(clusterManager.getHaManager().getAllNode())
            .flatMap(serverNode -> sendNotify("/plugin/uninstall", Mono.just(id)))
            .then();
    }

    void register(PluginProvider pluginProvider) {
        providers.put(pluginProvider.getId(), pluginProvider);
    }

    void register(PluginListener listener) {
        pluginListener.addListener(listener);
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean,
                                                 @Nonnull String beanName) throws BeansException {
        if (bean instanceof PluginProvider) {
            register(((PluginProvider) bean));
        }
        if (bean instanceof PluginListener) {
            register(((PluginListener) bean));
        }
        return bean;
    }

    @AllArgsConstructor
    private class DefaultPluginRunnerContext implements PluginRunnerContext {

        @Override
        public <T> Optional<T> getService(Class<T> service) {
            try {
                return Optional.of(applicationContext.getBean(service));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Optional.empty();
            }
        }

        @Override
        @SuppressWarnings("all")
        public <T> Optional<T> getService(String service) {
            try {
                return Optional.of((T) applicationContext.getBean(service));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Optional.empty();
            }
        }

        @Override
        public <T> List<T> getServices(Class<T> service) {
            try {
                return new ArrayList<>(applicationContext.getBeansOfType(service).values());
            } catch (Exception e) {
                log.error("load service [{}] error", service, e);
                return Collections.emptyList();
            }
        }

    }

    @AllArgsConstructor
    private class ClusterPlugin implements ProxyPlugin {
        private final String id;

        private final Plugin plugin;

        @Override
        public Plugin getTarget() {
            return plugin;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Mono<Void> start() {
            return plugin
                .start()
                .then(
                    sendNotify("/plugin/start", plugin.getId())
                )
                .then(Mono.defer(() -> pluginListener.onStart(plugin)))
                .then();
        }

        @Override
        public Mono<Void> stop() {
            return plugin.stop()
                .then(
                    sendNotify("/plugin/stop", plugin.getId())
                )
                .then(Mono.defer(() -> pluginListener.onStop(plugin)))
                .then();
        }
    }
}
