package org.jetlinks.pro.plugin.supports;

import org.jetlinks.pro.plugin.Plugin;
import org.jetlinks.pro.plugin.PluginListener;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

public class CompositePluginListener implements PluginListener {

    private final List<PluginListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(PluginListener listener) {
        listeners.add(listener);
    }

    @Override
    public Mono<Plugin> onInstall(Plugin plugin) {
        return doListener(plugin, PluginListener::onInstall);
    }

    @Override
    public Mono<Plugin> onUninstall(Plugin plugin) {
        return doListener(plugin, PluginListener::onUninstall);
    }

    @Override
    public Mono<Plugin> onStart(Plugin plugin) {
        return doListener(plugin, PluginListener::onStart);
    }

    @Override
    public Mono<Plugin> onStop(Plugin plugin) {
        return doListener(plugin, PluginListener::onStop);
    }

    protected Mono<Plugin> doListener(Plugin plugin, BiFunction<PluginListener, Plugin, Mono<Plugin>> func) {
        Mono<Plugin> source = Mono.just(plugin);

        for (PluginListener listener : listeners) {
            source = source.flatMap(plg -> func.apply(listener, plg));
        }

        return source;
    }

}
