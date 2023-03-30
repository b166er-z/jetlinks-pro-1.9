package org.jetlinks.pro.plugin;

import reactor.core.publisher.Mono;

public interface ProxyPlugin extends Plugin {

    Plugin getTarget();

    @Override
    default String getId() {
        return getTarget().getId();
    }

    @Override
    default PluginDescription getDescription() {
        return getTarget().getDescription();
    }

    @Override
    default boolean isReadonly() {
        return getTarget().isReadonly();
    }

    @Override
    default PluginState getState() {
        return getTarget().getState();
    }

    default Mono<Void> start() {
        return getTarget().start();
    }

    default Mono<Void> stop() {
        return getTarget().stop();
    }

    default Mono<Void> destroy() {
        return getTarget().destroy();
    }

    @Override
    default Mono<ExecutablePlugin> asExecutable() {
        return getTarget().asExecutable();
    }
}
