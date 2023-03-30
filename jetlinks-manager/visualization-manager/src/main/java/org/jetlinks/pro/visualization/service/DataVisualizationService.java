package org.jetlinks.pro.visualization.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.pro.visualization.entity.DataVisualizationEntity;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Service
public class DataVisualizationService extends GenericReactiveCrudService<DataVisualizationEntity, String> {

    @Override
    public Mono<SaveResult> save(Publisher<DataVisualizationEntity> entityPublisher) {
        return Flux.from(entityPublisher)
                   .doOnNext(DataVisualizationEntity::applyId)
                   .as(super::save);
    }

    @Override
    public Mono<Integer> insert(Publisher<DataVisualizationEntity> entityPublisher) {
        return Flux.from(entityPublisher)
                   .doOnNext(DataVisualizationEntity::applyId)
                   .as(super::insert);
    }

    @Override
    public Mono<Integer> insertBatch(Publisher<? extends Collection<DataVisualizationEntity>> entityPublisher) {
        return Flux.from(entityPublisher)
                   .doOnNext(arr -> arr.forEach(DataVisualizationEntity::applyId))
                   .as(super::insertBatch);
    }
}
