package org.jetlinks.pro.device.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.pro.device.entity.ProtocolSupportEntity;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportManager;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class LocalProtocolSupportService extends GenericReactiveCrudService<ProtocolSupportEntity, String> {

    @Autowired
    private ProtocolSupportManager supportManager;

    @Autowired
    private ProtocolSupportLoader loader;

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
            .flatMap(id -> supportManager.remove(id).thenReturn(id))
            .as(super::deleteById)
            ;
    }

    public Mono<Boolean> deploy(String id) {
        return findById(Mono.just(id))
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .map(ProtocolSupportEntity::toDeployDefinition)
            .flatMap(def -> loader.load(def).thenReturn(def))
            .onErrorMap(err -> new BusinessException("无法加载协议:" + err.getMessage(), err))
            .flatMap(supportManager::save)
            .flatMap(r -> createUpdate()
                .set(ProtocolSupportEntity::getState, 1)
                .where(ProtocolSupportEntity::getId, id)
                .execute())
            .map(i -> i > 0);
    }

    public Mono<Boolean> unDeploy(String id) {
        return findById(Mono.just(id))
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .map(ProtocolSupportEntity::toUnDeployDefinition)
            .flatMap(supportManager::save)
            .flatMap(r -> createUpdate()
                .set(ProtocolSupportEntity::getState, 0)
                .where(ProtocolSupportEntity::getId, id)
                .execute())
            .map(i -> i > 0);
    }

    @Transactional
    public Mono<Void> addProtocolsThenDeploy(List<ProtocolSupportEntity> entities) {
        return Flux.fromIterable(entities)
            .doOnNext(entity-> Assert.hasText(entity.getId(), "ID不能为空"))
            .as(this::save)
            .thenMany(Flux.fromIterable(entities))
            .flatMap(entity-> deploy(entity.getId()))
            .then();
    }

}
