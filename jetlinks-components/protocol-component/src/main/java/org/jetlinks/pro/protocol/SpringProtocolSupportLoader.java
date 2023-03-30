package org.jetlinks.pro.protocol;

import lombok.AllArgsConstructor;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 使用Spring管理协议加载器,实现{@link ProtocolSupportLoaderProvider}并注入到Spring即可。
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
@AllArgsConstructor
public class SpringProtocolSupportLoader implements ProtocolSupportLoader, BeanPostProcessor {

    private final Map<String, ProtocolSupportLoaderProvider> providers = new ConcurrentHashMap<>();

    private final EventBus eventBus;

    public void register(ProtocolSupportLoaderProvider provider) {
        this.providers.put(provider.getProvider(), provider);
    }

    @Override
    public Mono<? extends ProtocolSupport> load(ProtocolSupportDefinition definition) {
        return Mono.justOrEmpty(this.providers.get(definition.getProvider()))
                   .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("unsupported provider:" + definition
                       .getProvider())))
                   .flatMap((provider) -> provider.load(definition))
                   .map(loaded -> new RenameProtocolSupport(definition.getId(), definition.getName(), definition.getDescription(), loaded, eventBus));
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ProtocolSupportLoaderProvider) {
            register(((ProtocolSupportLoaderProvider) bean));
        }
        return bean;
    }
}
