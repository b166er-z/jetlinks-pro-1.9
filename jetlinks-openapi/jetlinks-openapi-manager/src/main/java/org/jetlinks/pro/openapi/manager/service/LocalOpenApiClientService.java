package org.jetlinks.pro.openapi.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.api.service.UserService;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.jetlinks.pro.openapi.manager.entity.OpenApiClientEntity;
import org.jetlinks.pro.openapi.manager.enums.DataStatus;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
@Slf4j
public class LocalOpenApiClientService extends GenericReactiveCacheSupportCrudService<OpenApiClientEntity, String> {

    @Autowired
    private ReactiveUserService userService;

    @Override
    public String getCacheName() {
        return "open-api-client";
    }

    @Override
    public Mono<Integer> insert(Publisher<OpenApiClientEntity> entityPublisher) {
        return Flux
            .from(entityPublisher)
            .flatMap(this::doSyncUser)
            .reduce(Math::addExact);
    }

    @Override
    public Mono<SaveResult> save(Publisher<OpenApiClientEntity> entityPublisher) {
        return Flux
            .from(entityPublisher)
            .flatMap(this::doSyncUser)
            .reduce(Math::addExact)
            .map(i -> SaveResult.of(0, i));
    }

    protected Mono<Integer> doSyncUser(OpenApiClientEntity entity) {
        return findById(entity.getId())
            .flatMap(old -> super.updateById(old.getId(), Mono.just(entity)))
            .switchIfEmpty(Mono.defer(() -> userService
                .findByUsername(entity.getUsername())
                .flatMap(user -> Mono.error(new BusinessException("用户已存在,请勿重复添加!")))
                .switchIfEmpty(Mono.defer(() -> {
                    UserEntity userEntity = new UserEntity();
                    userEntity.setName(entity.getClientName());
                    userEntity.setUsername(entity.getUsername());
                    userEntity.setPassword(entity.getPassword());
                    userEntity.setStatus(DataStatus.STATUS_ENABLED);
                    return userService
                        .saveUser(Mono.just(userEntity))
                        .doOnNext(b -> entity.setUserId(userEntity.getId()))
                        .then(super.save(Mono.just(entity)));
                })).thenReturn(1)));


    }
}
