package org.jetlinks.pro.plugin;

import reactor.core.publisher.Flux;

public interface PluginProviderLoader {

    Flux<PluginProvider> loadProvider(PluginPackage pluginPackage);

}
