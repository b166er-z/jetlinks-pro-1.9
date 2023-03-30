package org.jetlinks.pro.gateway.spring;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@Slf4j
@AllArgsConstructor
public class SpringMessageBroker implements BeanPostProcessor {

    private final EventBus eventBus;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> type = ClassUtils.getUserClass(bean);
        ReflectionUtils.doWithMethods(type, method -> {
            AnnotationAttributes subscribes = AnnotatedElementUtils.getMergedAnnotationAttributes(method, Subscribe.class);
            if (CollectionUtils.isEmpty(subscribes)) {
                return;
            }
            String id = subscribes.getString("id");
            if (!StringUtils.hasText(id)) {
                id = type.getSimpleName().concat(".").concat(method.getName());
            }

            Subscription subscription = Subscription
                .builder()
                .subscriberId("spring:" + id)
                .topics(Arrays.stream(subscribes.getStringArray("value"))
                              .flatMap(topic -> TopicUtils.expand(topic).stream())
                              .collect(Collectors.toList())
                )
                .features((Subscription.Feature[]) subscribes.get("features"))
                .build();

            ProxyMessageListener listener = new ProxyMessageListener(bean, method);

            eventBus
                .subscribe(subscription)
                .doOnNext(msg -> {
                    try {
                        listener
                            .onMessage(msg)
                            .doOnEach(ReactiveLogger.onError(error -> log.error("handle[{}] event message error", listener
                                .toString(), error)))
                            .subscribe();
                    } catch (Throwable e) {
                        log.error("handle[{}] event message error", listener.toString(), e);
                    }
                })
                .subscribe();

        });

        return bean;
    }

}
