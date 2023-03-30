package org.jetlinks.pro.plugin;

import reactor.core.publisher.Mono;

public interface PluginListener {

    Mono<Plugin> onInstall(Plugin plugin);

    Mono<Plugin> onUninstall(Plugin plugin);

    Mono<Plugin> onStart(Plugin plugin);

    Mono<Plugin> onStop(Plugin plugin);

}
