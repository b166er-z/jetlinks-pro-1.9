package org.jetlinks.pro.notify.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.jetlinks.pro.notify.NotifyType;
import org.jetlinks.pro.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.pro.notify.template.AbstractTemplateManager;
import org.jetlinks.pro.notify.template.TemplateProperties;
import org.jetlinks.pro.notify.template.TemplateProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class DefaultTemplateManager extends AbstractTemplateManager implements BeanPostProcessor {

    @Autowired
    private NotifyTemplateService templateService;

    @Override
    protected Mono<TemplateProperties> getProperties(NotifyType type, String id) {
        return templateService.findById(Mono.just(id))
                .map(NotifyTemplateEntity::toTemplateProperties);
    }
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof TemplateProvider) {
            register(((TemplateProvider) bean));
        }
        return bean;
    }

}
