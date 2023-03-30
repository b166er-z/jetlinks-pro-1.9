package org.jetlinks.pro.plugin;

import reactor.core.publisher.Mono;

public interface Plugin {

    String getId();

    PluginDescription getDescription();

    Mono<Void> start();

    Mono<Void> stop();

    Mono<Void> destroy();

    PluginState getState();

    boolean isReadonly();

    default Mono<ExecutablePlugin> asExecutable() {
        if (this instanceof ExecutablePlugin) {
            return Mono.just(((ExecutablePlugin) this));
        }
        return Mono.empty();
    }
}
