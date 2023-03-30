package org.jetlinks.pro.device.service;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.pro.PropertyConstants;
import org.jetlinks.pro.device.entity.*;
import org.jetlinks.pro.device.events.DeviceDeployedEvent;
import org.jetlinks.pro.device.service.term.DeviceGroupTerm;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeviceGroupService extends GenericReactiveTreeSupportCrudService<DeviceGroupEntity, String> {

    @SuppressWarnings("all")
    @Autowired
    private ReactiveRepository<DeviceGroupBindEntity, String> bindRepository;

    @Autowired
    private DeviceRegistry deviceRegistry;

    @Autowired
    private LocalDeviceInstanceService instanceService;

    public Mono<PagerResult<DeviceGroupInfo>> queryGroupInfo(QueryParamEntity param) {
        return this
            .queryPager(param)
            .filter(result -> result.getTotal() > 0)
            .flatMap(result -> {
                //查询出关联信息
                return this
                    .convertGroupInfo(result.getData())
                    .collectList()
                    .map(list -> PagerResult.of(result.getTotal(), new ArrayList<>(list), param));
            })
            .defaultIfEmpty(PagerResult.of(0, Collections.emptyList(), param));
    }

    private Flux<DeviceGroupInfo> convertGroupInfo(List<DeviceGroupEntity> list) {
        Map<String, DeviceGroupInfo> groupMap = list
            .stream()
            .collect(Collectors.toMap(DeviceGroupEntity::getId, DeviceGroupInfo::of));
        //查询出关联信息
        return bindRepository
            .createQuery()
            .where()
            .in(DeviceGroupBindEntity::getGroupId, groupMap.keySet())
            .fetch()
            //按设备ID分组,key为设备ID,value为设备对应的分组ID集合
            .collect(Collectors.groupingBy(
                DeviceGroupBindEntity::getDeviceId,
                Collectors.mapping(DeviceGroupBindEntity::getGroupId, Collectors.toList()))
            )
            .flatMap(group -> instanceService
                .findById(group.keySet())
                .doOnNext(instance -> {
                    DeviceBasicInfo basicInfo = DeviceBasicInfo.of(instance);
                    for (String groupId : group.get(instance.getId())) {
                        groupMap.get(groupId).getDevices().add(basicInfo);
                    }
                }).then())
            .thenMany(Flux.fromIterable(groupMap.values()));
    }

    public Mono<DeviceGroupInfo> findGroupInfo(String groupId) {
        return findById(groupId)
            .flatMap(entity -> convertGroupInfo(Collections.singletonList(entity)).last());
    }

    public Mono<Void> bind(String groupId, Collection<String> deviceIdList) {
        return Flux.fromIterable(deviceIdList)
                   .map(deviceId -> DeviceGroupBindEntity.of(groupId, deviceId))
                   .as(bindRepository::save)
                   .map(SaveResult::getTotal)
                   .then(syncDeviceGroup(deviceIdList));
    }

    public Mono<Void> unbind(String groupId, Collection<String> deviceIdList) {
        return bindRepository.createDelete()
                             .where(DeviceGroupBindEntity::getGroupId, groupId)
                             .in(DeviceGroupBindEntity::getDeviceId, deviceIdList)
                             .execute()
                             .then(syncDeviceGroup(deviceIdList));
    }

    public Mono<Void> unbind(String groupId) {
        return bindRepository
            .createQuery()
            .where(DeviceGroupBindEntity::getGroupId, groupId)
            .fetch()
            .map(DeviceGroupBindEntity::getGroupId)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(list -> bindRepository
                .createDelete()
                .where(DeviceGroupBindEntity::getGroupId, groupId)
                .execute()
                .then(syncDeviceGroup(list)));
    }

    public Mono<Void> cleanDeviceGroup(Collection<String> deviceIds) {
        return Flux
            .fromIterable(deviceIds)
            .flatMap(deviceRegistry::getDevice)
            .flatMap(device -> device.removeConfig(PropertyConstants.groupId.getKey()))
            .then();
    }

    public Mono<Void> syncDeviceGroup(Collection<String> deviceIds) {
        List<String> notInGroup = new ArrayList<>(deviceIds);
        return bindRepository
            .createQuery()
            .in(DeviceGroupBindEntity::getDeviceId, deviceIds)
            .fetch()
            .groupBy(DeviceGroupBindEntity::getDeviceId,Integer.MAX_VALUE)
            .flatMap(group -> {
                String deviceId = group.key();
                notInGroup.remove(deviceId);
                return Mono
                    .zip(deviceRegistry.getDevice(deviceId), group.map(DeviceGroupBindEntity::getGroupId).collectList())
                    .flatMap(tp2 -> tp2.getT1().setConfig(PropertyConstants.groupId, tp2.getT2()));
            })
            .then(cleanDeviceGroup(notInGroup));
    }


    //激活设备时
    @EventListener
    public void handleDeviceDeploy(DeviceDeployedEvent event) {
        syncDeviceGroup(
            event.getDevices().stream().map(DeviceInstanceEntity::getId).collect(Collectors.toList())
        ).subscribe();
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
                   .flatMap(id -> unbind(id).thenReturn(id))
                   .as(super::deleteById);
    }

    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.SNOW_FLAKE_STRING;
    }

    @Override
    public void setChildren(DeviceGroupEntity entity, List<DeviceGroupEntity> children) {
        entity.setChildren(children);
    }
}
