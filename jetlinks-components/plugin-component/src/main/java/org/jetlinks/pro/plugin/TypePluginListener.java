package org.jetlinks.pro.plugin;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@AllArgsConstructor
public abstract class TypePluginListener<T extends Plugin> implements PluginListener {

    private final Class<T> type;

    @Override
    public Mono<Plugin> onInstall(Plugin plugin) {
        return doListener(plugin, this::doOnInstall);
    }

    @Override
    public Mono<Plugin> onUninstall(Plugin plugin) {
        return doListener(plugin, this::doOnUninstall);
    }

    @Override
    public Mono<Plugin> onStart(Plugin plugin) {
        return doListener(plugin, this::doOnStart);
    }

    @Override
    public Mono<Plugin> onStop(Plugin plugin) {
        return doListener(plugin, this::doOnStop);
    }

    private Mono<Plugin> doListener(Plugin plugin, Function<T, Mono<Plugin>> function) {
        if (type.isInstance(plugin)) {
            return function.apply(type.cast(plugin));
        }
        return Mono.just(plugin);
    }

    protected Mono<Plugin> doOnInstall(T plugin) {
        return Mono.just(plugin);
    }

    protected Mono<Plugin> doOnUninstall(T plugin) {
        return Mono.just(plugin);
    }

    protected Mono<Plugin> doOnStart(T plugin) {
        return Mono.just(plugin);
    }

    protected Mono<Plugin> doOnStop(T plugin) {
        return Mono.just(plugin);
    }

}
