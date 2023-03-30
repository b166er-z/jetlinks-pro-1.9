package org.jetlinks.pro.device.message.writer;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.core.Values;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.GeoType;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.entity.DeviceTagEntity;
import org.jetlinks.pro.device.events.DeviceDeployedEvent;
import org.jetlinks.pro.gateway.DeviceMessageUtils;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.geo.GeoObject;
import org.jetlinks.pro.geo.GeoObjectManager;
import org.jetlinks.pro.geo.GeoQueryParameter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 地理位置数据
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
@Slf4j
@ConditionalOnClass(GeoObjectManager.class)
@ConditionalOnBean(GeoObjectManager.class)
public class DeviceGeoDataWriter {

    private final GeoObjectManager geoObjectManager;

    private final DeviceRegistry registry;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    public DeviceGeoDataWriter(GeoObjectManager geoObjectManager,
                               DeviceRegistry registry,
                               ReactiveRepository<DeviceTagEntity, String> tagRepository) {
        this.geoObjectManager = geoObjectManager;
        this.registry = registry;
        this.tagRepository = tagRepository;
    }

    private Mono<PropertyMetadata> findGeoProperty(DeviceMetadata metadata) {
        return Mono
            .justOrEmpty(metadata
                             .getProperties()
                             .stream()
                             .filter(prop -> prop.getValueType() instanceof GeoType)
                             .findFirst());
    }

    @Subscribe("/device/*/*/message/property/**")
    public Mono<Void> handleDeviceGeoProperty(DeviceMessage msg) {
        Map<String, Object> properties = DeviceMessageUtils.tryGetProperties(msg).orElse(Collections.emptyMap());

        if (properties.isEmpty()) {
            return Mono.empty();
        }
        return registry
            .getDevice(msg.getDeviceId())
            .flatMap(device -> Mono
                .zip(
                    Mono.just(device),
                    device.getMetadata()
                          .flatMap(this::findGeoProperty)
                          .filter(prop -> properties.get(prop.getId()) != null),
                    device.getProduct()
                ))
            .flatMap(devMeta -> {
                DeviceOperator device = devMeta.getT1();
                PropertyMetadata metadata = devMeta.getT2();
                DeviceProductOperator product = devMeta.getT3();

                GeoObject geoObject = new GeoObject();
                geoObject.setId(msg.getDeviceId().concat(":").concat(metadata.getId()));
                geoObject.setPoint(GeoType.GLOBAL.convert(properties.get(metadata.getId())));
                geoObject.setProperty(metadata.getId());
                geoObject.setObjectId(msg.getDeviceId());
                geoObject.setTimestamp(msg.getTimestamp());
                geoObject.setObjectType("device");
                Map<String, Object> tags = new HashMap<>();
                tags.put("productId", product.getId());
                tags.put("from", "property");
                msg.getHeader("orgId").map(String::valueOf)
                   .ifPresent(str -> tags.put("orgId", str));
                msg.getHeader("deviceName").map(String::valueOf)
                   .ifPresent(str -> tags.put("deviceName", str));

                geoObject.setTags(tags);

                return geoObjectManager.commit(Collections.singleton(geoObject));
            });
    }

    @EventListener
    public void handleDeviceTagEvent(EntityCreatedEvent<DeviceTagEntity> event) {
        saveTagGeo(event.getEntity());
    }

    @EventListener
    public void handleDeviceTagEvent(EntitySavedEvent<DeviceTagEntity> event) {
        saveTagGeo(event.getEntity());
    }

    @EventListener
    public void handleDeviceTagEvent(EntityDeletedEvent<DeviceTagEntity> event) {
        geoObjectManager
            .remove(event.getEntity()
                         .stream()
                         .map(DeviceTagEntity::getId)
                         .collect(Collectors.toList()))
            .subscribe();
    }

    @EventListener
    public void handleDeviceEvent(EntityDeletedEvent<DeviceInstanceEntity> event) {
        new GeoQueryParameter()
            .filter(query -> query.in(
                GeoObject::getObjectId,
                event.getEntity()
                     .stream()
                     .map(DeviceInstanceEntity::getId)
                     .collect(Collectors.toList())))
            .as(geoObjectManager::remove)
            .subscribe(
                (r) -> log.debug("delete device geo [{}] objects", r),
                (err) -> log.error("delete device geo objects error", err)
            );
    }

    @EventListener
    public void handleDeviceTagEvent(EntityModifyEvent<DeviceTagEntity> event) {
        saveTagGeo(event.getAfter());
    }


    @EventListener
    public void handleDeviceDeployEvent(DeviceDeployedEvent event) {
        //激活设备时，同步设备标签中的地理位置信息
        tagRepository.createQuery()
                     .in(DeviceTagEntity::getDeviceId, event
                         .getDevices()
                         .stream()
                         .map(DeviceInstanceEntity::getId)
                         .collect(Collectors.toSet()))
                     .and(DeviceTagEntity::getType, "geoPoint")
                     .fetch()
                     .as(this::saveTagGeo)
                     .subscribe();
    }

    public void saveTagGeo(Collection<DeviceTagEntity> tagEntities) {
        saveTagGeo(Flux.fromIterable(tagEntities))
            .subscribe();
    }

    public Mono<Void> saveTagGeo(Flux<DeviceTagEntity> tagEntities) {
        return tagEntities
            .filter(tag -> "geo".equals(tag.getType()) || "geoPoint".equals(tag.getType()))
            .flatMap(tag -> {
                GeoObject geoObject = new GeoObject();
                geoObject.setId(tag.getId());
                geoObject.setPoint(GeoType.GLOBAL.convert(tag.getValue()));
                geoObject.setProperty(tag.getKey());
                geoObject.setObjectType("device");
                geoObject.setObjectId(tag.getDeviceId());
                geoObject.setTimestamp(System.currentTimeMillis());
                return registry
                    .getDevice(tag.getDeviceId())
                    .flatMap(device -> device
                        .getSelfConfigs(DeviceConfigKey.productId.getKey(), "productName", "deviceName", "orgId")
                        .map(Values::getAllValues)
                        .doOnNext(tags -> {
                            tags.put("from", "tag");
                            geoObject.setTags(tags);
                        })
                        //为什么在这里return而不在外面?
                        //因为设备未激活时,无法获取到一些重要信息,所以未empty的话就不同步了.
                        .thenReturn(geoObject)
                    );
            })
            .collectList()
            .flatMap(geoObjectManager::commit)
            .doOnError(err -> log.error(err.getMessage(), err))
            .then();

    }

}
