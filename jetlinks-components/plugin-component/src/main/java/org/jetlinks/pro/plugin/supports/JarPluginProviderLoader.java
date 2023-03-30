package org.jetlinks.pro.plugin.supports;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.plugin.PluginPackage;
import org.jetlinks.pro.plugin.PluginProvider;
import org.jetlinks.pro.plugin.PluginProviderLoader;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class JarPluginProviderLoader implements PluginProviderLoader {

    private final Map<String, PluginClassLoader> protocolLoaders = new ConcurrentHashMap<>();

    @Override
    public Flux<PluginProvider> loadProvider(PluginPackage pluginPackage) {

        return Flux.defer(() -> {
            try {

                ValueObject config = ValueObject.of(pluginPackage.getLoaderConfigs());

                String location = config.getString("location")
                    .orElseThrow(() -> new IllegalArgumentException("[loaderConfigs.location]不能为空"));

                if (!location.contains("://")) {
                    location = "file://" + location;
                }
                location = "jar:" + location + "!/";
                log.debug("load plugin provider from : {}", location);
                PluginClassLoader loader;
                PluginClassLoader old = protocolLoaders.put(pluginPackage.getId(), loader = new PluginClassLoader(location, this.getClass().getClassLoader()));
                if (null != old) {
                    old.close();
                }
                return Flux.fromIterable(
                    ServiceLoader.load(PluginProvider.class, loader)
                );
            } catch (Exception e) {
                return Mono.error(e);
            }
        })
            .subscribeOn(Schedulers.elastic())
            .timeout(Duration.ofSeconds(10), Mono.error(TimeoutException::new));
    }

}
