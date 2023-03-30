package org.jetlinks.pro.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.entity.DeviceOperationLogEntity;
import org.jetlinks.pro.device.entity.DeviceProductEntity;
import org.jetlinks.pro.device.entity.DevicePropertiesEntity;
import org.jetlinks.pro.device.enums.DeviceProductState;
import org.jetlinks.pro.device.events.DeviceProductDeployEvent;
import org.jetlinks.pro.device.measurements.message.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.pro.timeseries.TimeSeriesManager;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class LocalDeviceProductService extends GenericReactiveCrudService<DeviceProductEntity, String> {

    private final DeviceRegistry registry;

    private final ApplicationEventPublisher eventPublisher;

    private final ReactiveRepository<DeviceInstanceEntity, String> instanceRepository;

    private final TimeSeriesManager timeSeriesManager;

    @SuppressWarnings("all")
    public LocalDeviceProductService(DeviceRegistry registry,
                                     ApplicationEventPublisher eventPublisher,
                                     ReactiveRepository<DeviceInstanceEntity, String> instanceRepository,
                                     TimeSeriesManager timeSeriesManager) {
        this.registry = registry;
        this.eventPublisher = eventPublisher;
        this.instanceRepository = instanceRepository;
        this.timeSeriesManager = timeSeriesManager;
    }

    @Override
    public Mono<SaveResult> save(Publisher<DeviceProductEntity> entityPublisher) {
        return Flux.from(entityPublisher)
                   .doOnNext(prod -> prod.setState(null))
                   .as(super::save);
    }

    @Override
    public Mono<Integer> updateById(String id, Mono<DeviceProductEntity> entityPublisher) {
        return super.updateById(id, entityPublisher.doOnNext(e -> e.setState(null)));
    }

    public Mono<Integer> deploy(String id) {
        return findById(Mono.just(id))
            .flatMap(product -> registry
                .register(product.toProductInfo())
                .then(
                    createUpdate()
                        .set(DeviceProductEntity::getState, DeviceProductState.registered.getValue())
                        .where(DeviceProductEntity::getId, id)
                        .execute()
                )
                .flatMap(i -> FastBeanCopier
                    .copy(product, new DeviceProductDeployEvent())
                    .publish(eventPublisher)
                    .thenReturn(i))
            );
    }


    public Mono<Integer> cancelDeploy(String id) {
        return createUpdate()
            .set(DeviceProductEntity::getState, DeviceProductState.unregistered.getValue())
            .where(DeviceProductEntity::getId, id)
            .execute();

    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
                   .collectList()
                   .flatMap(idList ->
                                instanceRepository.createQuery()
                                                  .where()
                                                  .in(DeviceInstanceEntity::getProductId, idList)
                                                  .count()
                                                  .flatMap(i -> {
                                                      if (i > 0) {
                                                          return Mono.error(new IllegalArgumentException("存在关联设备,无法删除!"));
                                                      } else {
                                                          return super.deleteById(Flux.fromIterable(idList));
                                                      }
                                                  }));
    }

    @Deprecated
    public Mono<PagerResult<Map<String, Object>>> queryDeviceEvent(String productId,
                                                                   String eventId,
                                                                   QueryParamEntity entity,
                                                                   boolean format) {
        return registry
            .getProduct(productId)
            .flatMap(operator -> Mono.just(operator.getId()).zipWith(operator.getMetadata()))
            .flatMap(tp -> timeSeriesManager
                .getService(DeviceTimeSeriesMetric.deviceEventMetric(tp.getT1(), eventId))
                .queryPager(entity, data -> {
                    if (!format) {
                        return data.getData();
                    }
                    Map<String, Object> formatData = new HashMap<>(data.getData());
                    tp.getT2()
                      .getEvent(eventId)
                      .ifPresent(eventMetadata -> {
                          DataType type = eventMetadata.getType();
                          if (type instanceof ObjectType) {
                              @SuppressWarnings("all")
                              Map<String, Object> val = (Map<String, Object>) type.format(formatData);
                              val.forEach((k, v) -> formatData.put(k + "_format", v));
                          } else {
                              formatData.put("value_format", type.format(data.get("value")));
                          }
                      });
                    return formatData;
                })
            ).defaultIfEmpty(PagerResult.empty());
    }

    /**
     * 将产品的物模型合并到产品下设备的独立物模型中
     *
     * @param productId 产品ID
     * @return void
     */
    public Mono<Void> mergeMetadataToDevice(String productId) {
        return this
            .findById(productId)
            .flatMap(product -> JetLinksDeviceMetadataCodec.getInstance().decode(product.getMetadata()))
            .flatMap(metadata -> instanceRepository
                .createQuery()
                //查询出产品下配置了独立物模型的设备
                .where(DeviceInstanceEntity::getProductId, productId)
                .notNull(DeviceInstanceEntity::getDeriveMetadata)
                .notEmpty(DeviceInstanceEntity::getDeriveMetadata)
                .fetch()
                //合并物模型
                .flatMap(instance -> instance.mergeMetadata(metadata).thenReturn(instance))
                //更新物模型
                .flatMap(instance -> instanceRepository
                    .createUpdate()
                    .set(instance::getDeriveMetadata)
                    .where(instance::getId)
                    .execute()
                    .then(registry.getDevice(instance.getId()))
                    .flatMap(device -> device.updateMetadata(instance.getDeriveMetadata()))
                )
                .then());
    }

    @Deprecated
    public Mono<PagerResult<DevicePropertiesEntity>> queryDeviceProperties(String productId, QueryParamEntity entity) {
        return timeSeriesManager
            .getService(DeviceTimeSeriesMetric.devicePropertyMetric(productId))
            .queryPager(entity, data -> data.as(DevicePropertiesEntity.class))
            .defaultIfEmpty(PagerResult.empty());
    }

    @Deprecated
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(String productId, QueryParamEntity entity) {
        return timeSeriesManager
            .getService(DeviceTimeSeriesMetric.deviceLogMetric(productId))
            .queryPager(entity, data -> data.as(DeviceOperationLogEntity.class))
            .defaultIfEmpty(PagerResult.empty());
    }

}
