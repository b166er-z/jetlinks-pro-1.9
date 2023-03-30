package org.jetlinks.pro.device.service;

import org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService;
import org.jetlinks.core.device.manager.BindInfo;
import org.jetlinks.core.device.manager.DeviceBindManager;
import org.jetlinks.pro.device.entity.DeviceBindEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collection;

@Service
public class DeviceBindService extends GenericReactiveCacheSupportCrudService<DeviceBindEntity, String> implements DeviceBindManager {

    @Override
    public String getCacheName() {
        return "device-third-bind";
    }

    @Override
    public Mono<Void> bind(@Nonnull String type,
                           @Nonnull String key,
                           @Nonnull String deviceId,
                           String description) {

        return this
            .save(Mono.just(DeviceBindEntity.of(type, key, deviceId, description)))
            .then();
    }

    @Override
    public Mono<Void> bindBatch(@Nonnull String type, Collection<BindInfo> bindInfos) {
        return Flux.fromIterable(bindInfos)
                   .map(info -> DeviceBindEntity.of(type, info.getKey(), info.getDeviceId(), info.getDescription()))
                   .distinct(DeviceBindEntity::getId)
                   .as(this::save)
                   .then();
    }

    @Override
    public Mono<Void> unbind(@Nonnull String type, @Nonnull String key) {

        return this
            .deleteById(Mono.just(DeviceBindEntity.generateId(type, key)))
            .then();
    }

    @Override
    public Mono<Void> unbindByDevice(@Nonnull String type, @Nonnull Collection<String> deviceId) {
        Assert.hasText(type, "type can not be empty");
        Assert.notEmpty(deviceId, "deviceId can not be emtpy");
        return this
            .createDelete()
            .where(DeviceBindEntity::getType, type)
            .in(DeviceBindEntity::getDeviceId, deviceId)
            .execute()
            .then();
    }

    @Override
    public Mono<BindInfo> getBindInfo(@Nonnull String type, @Nonnull String key) {
        return this
            .findById(DeviceBindEntity.generateId(type, key))
            .map(DeviceBindEntity::toBindInfo);
    }

    @Override
    public Flux<BindInfo> getBindInfo(@Nonnull String type, @Nonnull Collection<String> keys) {

        return Flux
            .fromIterable(keys)
            .map(key -> DeviceBindEntity.generateId(type, key))
            .as(this::findById)
            .map(DeviceBindEntity::toBindInfo);
    }

    @Override
    public Mono<BindInfo> getBindInfoByDeviceId(@Nonnull String type, @Nonnull String deviceId) {
        Assert.hasText(type, "type can not be empty");
        Assert.hasText(deviceId, "deviceId can not be empty");

        return createQuery()
            .where(DeviceBindEntity::getType, type)
            .and(DeviceBindEntity::getDeviceId, deviceId)
            .fetchOne()
            .map(DeviceBindEntity::toBindInfo);
    }

    @Override
    public Flux<BindInfo> getBindInfoByDeviceId(@Nonnull String type, @Nonnull Collection<String> deviceId) {
        Assert.hasText(type, "type can not be empty");
        Assert.notEmpty(deviceId, "deviceId can not be empty");

        return createQuery()
            .where(DeviceBindEntity::getType, type)
            .in(DeviceBindEntity::getDeviceId, deviceId)
            .fetch()
            .map(DeviceBindEntity::toBindInfo);
    }

    @Override
    public Flux<BindInfo> getBindInfo(@Nonnull String type) {
        if (StringUtils.isEmpty(type)) {
            return Flux.empty();
        }
        return createQuery()
            .where(DeviceBindEntity::getType, type)
            .fetch()
            .map(DeviceBindEntity::toBindInfo);
    }
}
