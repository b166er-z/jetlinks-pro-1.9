package org.jetlinks.pro.notify.manager.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.notify.event.SerializableNotifierEvent;
import org.jetlinks.pro.notify.manager.entity.NotifyHistoryEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class NotifyHistoryService extends GenericReactiveCrudService<NotifyHistoryEntity, String> {


    @Subscribe("/notify/**")
    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> handleNotify(SerializableNotifierEvent event) {
        // TODO: 2020/4/10 进行重试
        return insert(Mono.just(NotifyHistoryEntity.of(event)))
            .then();
    }

}
