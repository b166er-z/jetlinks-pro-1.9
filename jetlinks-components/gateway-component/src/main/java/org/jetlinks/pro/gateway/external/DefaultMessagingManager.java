package org.jetlinks.pro.gateway.external;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DefaultMessagingManager implements MessagingManager, BeanPostProcessor {

    private final List<SubscriptionProvider> subProvider = new CopyOnWriteArrayList<>();

    private final static PathMatcher matcher = new AntPathMatcher();

    @Override
    public Flux<Message> subscribe(SubscribeRequest request) {

        return Flux.defer(() -> {
            for (SubscriptionProvider provider : subProvider) {
                for (String pattern : provider.getTopicPattern()) {
                    if (matcher.match(pattern, request.getTopic())) {
                        return provider
                            .subscribe(request)
                            .map(v -> {
                                if (v instanceof Message) {
                                    return ((Message) v);
                                }
                                return Message.success(request.getId(), request.getTopic(), v);
                            });
                    }
                }
            }
            return Flux.error(new UnsupportedOperationException("不支持的topic"));
        });
    }

    public void register(SubscriptionProvider provider) {
        subProvider.add(provider);
        subProvider.sort(Comparator.comparingInt(SubscriptionProvider::getOrder));
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        if (bean instanceof SubscriptionProvider) {
            register(((SubscriptionProvider) bean));
        }
        return bean;
    }
}
