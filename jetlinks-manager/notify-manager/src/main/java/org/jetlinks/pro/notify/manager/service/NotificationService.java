package org.jetlinks.pro.notify.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.notify.manager.entity.Notification;
import org.jetlinks.pro.notify.manager.entity.NotificationEntity;
import org.jetlinks.pro.notify.manager.enums.NotificationState;
import org.springframework.stereotype.Service;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService extends GenericReactiveCrudService<NotificationEntity, String> {


    private FluxSink<NotificationEntity> sink;

    @PostConstruct
    public void init() {

        FluxUtils
            .bufferRate(Flux.<NotificationEntity>create(sink -> this.sink = sink), 1000, 200, Duration.ofSeconds(3))
            .flatMap(buffer -> this
                .save(Flux.fromIterable(buffer))
                .onErrorResume(err -> {
                    log.error(err.getMessage(), err);
                    return Mono.empty();
                })
            )
            .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
            .subscribe()
        ;

    }

    @PreDestroy
    public void dispose(){
        sink.complete();
    }

    @Subscribe("/notifications/**")
    public Mono<Void> subscribeNotifications(Notification notification) {
        return Mono.fromRunnable(() -> sink.next(NotificationEntity.from(notification)));
    }

    public Flux<NotificationEntity> findAndMarkRead(QueryParamEntity query) {
        return this
            .query(query)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMapMany(list -> this
                .createUpdate()
                .set(NotificationEntity::getState, NotificationState.read)
                .where()
                .in(NotificationEntity::getId, list
                    .stream()
                    .map(NotificationEntity::getId)
                    .collect(Collectors.toList()))
                .and(NotificationEntity::getState, NotificationState.unread)
                .execute()
                .thenMany(Flux.fromIterable(list))
                .doOnNext(e -> e.setState(NotificationState.read)));
    }

}
