package org.jetlinks.pro.rule.engine.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.pro.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.pro.rule.engine.entity.DeviceAlarmHistoryEntity;
import org.jetlinks.pro.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.pro.rule.engine.enums.AlarmState;
import org.jetlinks.pro.rule.engine.model.DeviceAlarmModelParser;
import org.reactivestreams.Publisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DeviceAlarmService extends GenericReactiveCrudService<DeviceAlarmEntity, String> {

    private final RuleInstanceService instanceService;

    @SuppressWarnings("all")
    public DeviceAlarmService(RuleInstanceService instanceService) {
        this.instanceService = instanceService;
    }

    public Mono<Void> start(String id) {
        return findById(id)
            .flatMap(this::doStart);
    }

    @Override
    public Mono<SaveResult> save(Publisher<DeviceAlarmEntity> entityPublisher) {
        return Flux.from(entityPublisher)
            .doOnNext(e -> e.setState(null))
            .flatMap(alarm -> instanceService
                .save(Mono.just(alarm.toRuleInstance()))
                .thenReturn(alarm))
            .as(DeviceAlarmService.super::save);
    }

    public Mono<Void> stop(String id) {
        return instanceService
            .stop(id)
            .then(createUpdate()
                .set(DeviceAlarmEntity::getState, AlarmState.stopped)
                .where(DeviceAlarmEntity::getId, id)
                .execute())
            .then();
    }

    private Mono<Void> doStart(DeviceAlarmEntity entity) {
        return instanceService
            .save(Mono.just(entity.toRuleInstance()))
            .then(instanceService.start(entity.getId()))
            .then(createUpdate()
                .set(DeviceAlarmEntity::getState, AlarmState.running)
                .where(entity::getId).execute())
            .then();
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux
            .from(idPublisher)
            .flatMap(id -> instanceService.stop(id)
                .then(instanceService.deleteById(Mono.just(id)))
                .then(DeviceAlarmService.super.deleteById(Mono.just(id)))
            ).reduce(Math::addExact);
    }

    //删除规则实例时,删除告警配置
    @EventListener
    public void handleRuleInstanceRemove(EntityDeletedEvent<RuleInstanceEntity> event){
        event.async(
            Flux.fromIterable(event.getEntity())
            .filter(instance-> DeviceAlarmModelParser.format.equals(instance.getModelType()))
            .map(RuleInstanceEntity::getId)
            .as(this::deleteById)
            .then(

            )
        );
    }
}
