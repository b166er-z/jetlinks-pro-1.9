package org.jetlinks.pro.plugin;

import org.jetlinks.core.metadata.PropertyMetadata;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PluginProvider {

    String getId();

    String getName();

    List<PropertyMetadata> getConfigMetadata();

    Mono<Plugin> createPlugin(PluginPackage pluginPackage, PluginRunnerContext context);

}
