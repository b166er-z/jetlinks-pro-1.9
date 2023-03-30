package org.jetlinks.pro.rule.engine.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.pro.rule.engine.entity.DeviceAlarmHistoryEntity;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeviceAlarmHistoryService extends GenericReactiveCrudService<DeviceAlarmHistoryEntity, String> {


    private FluxSink<DeviceAlarmHistoryEntity> sink;

    @PostConstruct
    public void init() {
        FluxUtils
            .bufferRate(Flux.<DeviceAlarmHistoryEntity>create(sink -> this.sink = sink), 500, 200, Duration.ofSeconds(2))
            .publishOn(Schedulers.boundedElastic())
            .flatMap(list -> this
                .insertBatch(Mono.just(list))
                .onErrorResume(err -> {
                    log.error("save device alarm data error", err);
                    return Mono.empty();
                }))
            .subscribe();
    }

    @PreDestroy
    public void shutdown() {
        sink.complete();
    }

    @Subscribe("/rule-engine/device/alarm/**")
    public Mono<Void> saveAlarm(Map<String, Object> message) {
        DeviceAlarmHistoryEntity entity = FastBeanCopier.copy(message, DeviceAlarmHistoryEntity::new);
        if (message.containsKey("timestamp")) {
            entity.setAlarmTime(CastUtils.castDate(message.get("timestamp")));
        }
        entity.setAlarmData(message);
        return Mono
            .fromRunnable(() -> sink.next(entity));
    }

    //删除告警时，删除告警记录
    @EventListener
    public void handleDeviceAlarmRemoved(EntityDeletedEvent<DeviceAlarmEntity> event) {
        event.async(
            createDelete()
                .where()
                .in(DeviceAlarmHistoryEntity::getAlarmId, event
                    .getEntity()
                    .stream()
                    .map(DeviceAlarmEntity::getId)
                    .collect(Collectors.toSet()))
                .execute()
        );
    }

}
