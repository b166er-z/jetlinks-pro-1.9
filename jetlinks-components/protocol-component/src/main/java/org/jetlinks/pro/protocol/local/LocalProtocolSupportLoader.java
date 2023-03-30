package org.jetlinks.pro.protocol.local;

import lombok.AllArgsConstructor;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;

@AllArgsConstructor
@Component
@Profile("dev")
public class LocalProtocolSupportLoader implements ProtocolSupportLoaderProvider {

    private final ServiceContext serviceContext;

    @Override
    public String getProvider() {
        return "local";
    }

    @Override
    public Mono<LocalFileProtocolSupport> load(ProtocolSupportDefinition definition) {

        return Mono
            .fromCallable(() -> {
                ValueObject config = ValueObject.of(definition.getConfiguration());

                String location = config
                    .getString("location")
                    .orElseThrow(() -> new IllegalArgumentException("location cannot be null"));
                String provider = config.get("provider")
                    .map(String::valueOf)
                    .map(String::trim)
                    .orElseThrow(() -> new IllegalArgumentException("provider cannot be null"));
                File file = new File(location);
                if (!file.exists()) {
                    throw new FileNotFoundException("文件" + file.getName() + "不存在");
                }

                LocalFileProtocolSupport support = new LocalFileProtocolSupport();
                support.init(file, serviceContext, provider);
                return support;
            })
            .subscribeOn(Schedulers.boundedElastic());
    }
}
