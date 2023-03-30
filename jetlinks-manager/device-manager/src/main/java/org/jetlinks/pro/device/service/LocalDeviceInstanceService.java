package org.jetlinks.pro.device.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveDelete;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.event.GenericsPayloadApplicationEvent;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.FunctionInvokeMessageSender;
import org.jetlinks.core.message.ReadPropertyMessageSender;
import org.jetlinks.core.message.WritePropertyMessageSender;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.MergeOption;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.pro.device.entity.*;
import org.jetlinks.pro.device.enums.DeviceState;
import org.jetlinks.pro.device.events.DeviceDeployedEvent;
import org.jetlinks.pro.device.events.LedDeployedEvent;
import org.jetlinks.pro.device.web.response.DeviceDeployResult;
import org.jetlinks.pro.utils.ErrorUtils;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalDeviceInstanceService extends GenericReactiveCrudService<DeviceInstanceEntity, String> {

    private final DeviceRegistry registry;

    private final LocalDeviceProductService deviceProductService;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final DeviceBindService bindService;

    private final DeviceConfigMetadataManager metadataManager;

    public LocalDeviceInstanceService(DeviceRegistry registry,
                                      LocalDeviceProductService deviceProductService,
                                      @SuppressWarnings("all")
                                          ReactiveRepository<DeviceTagEntity, String> tagRepository,
                                      ApplicationEventPublisher eventPublisher,
                                      DeviceBindService bindService,
                                      DeviceConfigMetadataManager metadataManager) {
        this.registry = registry;
        this.deviceProductService = deviceProductService;
        this.tagRepository = tagRepository;
        this.eventPublisher = eventPublisher;
        this.bindService = bindService;
        this.metadataManager = metadataManager;
    }



    @Override
    public Mono<SaveResult> save(Publisher<DeviceInstanceEntity> entityPublisher) {
        return Flux
            .from(entityPublisher)
            .doOnNext(instance -> instance.setState(null))
            .as(super::save);
    }

    /**
     * 重置设备配置
     *
     * @param deviceId 设备ID
     * @return 重置后的配置
     * @since 1.2
     */
    public Mono<Map<String, Object>> resetConfiguration(String deviceId) {
        return this
            .findById(deviceId)
            .zipWhen(device -> deviceProductService.findById(device.getProductId()))
            .flatMap(tp2 -> {
                DeviceProductEntity product = tp2.getT2();
                DeviceInstanceEntity device = tp2.getT1();
                return Mono
                    .defer(() -> {
                        if (MapUtils.isNotEmpty(product.getConfiguration())) {
                            if (MapUtils.isNotEmpty(device.getConfiguration())) {
                                product.getConfiguration()
                                       .keySet()
                                       .forEach(device.getConfiguration()::remove);
                            }
                            //重置注册中心里的配置
                            return registry
                                .getDevice(deviceId)
                                .flatMap(opts -> opts.removeConfigs(product.getConfiguration().keySet()))
                                .then();
                        }
                        return Mono.empty();
                    })
                    .then(
                        //更新数据库
                        createUpdate()
                            .set(device::getConfiguration)
                            .where(device::getId)
                            .execute()
                    )
                    .thenReturn(device.getConfiguration());
            })
            .defaultIfEmpty(Collections.emptyMap())
            ;
    }

    /**
     * 发布设备到设备注册中心
     *
     * @param id 设备ID
     * @return 发布结果
     */
    public Mono<DeviceDeployResult> deploy(String id) {
        return findById(id)
            .flux()
            .as(this::deploy)
            .singleOrEmpty();
    }

    /**
     * 批量发布设备到设备注册中心
     *
     * @param flux 设备实例流
     * @return 发布数量
     */
    public Flux<DeviceDeployResult> deploy(Flux<DeviceInstanceEntity> flux) {
        return flux
            .flatMap(instance -> registry
                .register(instance.toDeviceInfo())
                .flatMap(deviceOperator -> deviceOperator
                    .checkState()//激活时检查设备状态
                    .onErrorReturn(org.jetlinks.core.device.DeviceState.offline)
                    .flatMap(r -> {
                        if (r.equals(org.jetlinks.core.device.DeviceState.unknown) ||
                            r.equals(org.jetlinks.core.device.DeviceState.noActive)) {
                            instance.setState(DeviceState.offline);
                            return deviceOperator.putState(org.jetlinks.core.device.DeviceState.offline);
                        }
                        instance.setState(DeviceState.of(r));
                        return Mono.just(true);
                    })
                    .flatMap(success -> success ? Mono.just(deviceOperator) : Mono.empty())
                )
                .thenReturn(instance))
            .buffer(200)//每200条数据批量更新
            .publishOn(Schedulers.single())
            .concatMap(all -> Flux
                .fromIterable(all)
                .groupBy(DeviceInstanceEntity::getState)
                .flatMap(group -> group
                    .map(DeviceInstanceEntity::getId)
                    .collectList()
                    .flatMap(list -> createUpdate()
                        .where()
                        .set(DeviceInstanceEntity::getState, group.key())
                        .set(DeviceInstanceEntity::getRegistryTime, new Date())
                        .in(DeviceInstanceEntity::getId, list)
                        .execute()
                        .map(r -> DeviceDeployResult.success(list.size()))
                        .onErrorResume(err -> Mono.just(DeviceDeployResult.error(err.getMessage())))))
                //推送激活事件
                .flatMap(res -> DeviceDeployedEvent.of(all).publish(eventPublisher).thenReturn(res))
                .flatMap(res -> LedDeployedEvent.of(Flux.fromIterable(all)).publish(eventPublisher).thenReturn(res))
            );
    }

    /**
     * 注销设备,取消后,设备无法再连接到服务. 注册中心也无法再获取到该设备信息.
     *
     * @param id 设备ID
     * @return 注销结果
     */
    public Mono<Integer> unregisterDevice(String id) {
        return this
            .findById(Mono.just(id))
            .flatMap(device -> this
                .createUpdate()
                .set(DeviceInstanceEntity::getState, DeviceState.notActive.getValue())
                .where(DeviceInstanceEntity::getId, id)
                .execute())
            .flatMap(res -> LedDeployedEvent.of(findById(Mono.just(id)).flux()).publish(eventPublisher))
            .then(
                registry
                    .unregisterDevice(id)
                    .onErrorResume(err -> Mono.empty())
            )
            .thenReturn(1);
    }

    /**
     * 批量注销设备
     *
     * @param ids 设备ID
     * @return 注销结果
     */
    public Mono<Integer> unregisterDevice(Publisher<String> ids) {
        return Flux.from(ids)
                   .flatMap(id -> registry.unregisterDevice(id).thenReturn(id))
                   .collectList()
                   .flatMap(list -> createUpdate()
                       .set(DeviceInstanceEntity::getState, DeviceState.notActive.getValue())
                       .where().in(DeviceInstanceEntity::getId, list)
                       .execute()
                        .flatMap(res -> LedDeployedEvent.of(findById(Flux.from(ids)))
                                                .publish(eventPublisher)
                                                .thenReturn(res)));
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
                   .collectList()
                   .flatMap(list -> createDelete()
                       .where()
                       .in(DeviceInstanceEntity::getId, list)
                       .and(DeviceInstanceEntity::getState, DeviceState.notActive)
                       .execute());
    }

    @Override
    public ReactiveDelete createDelete() {
        return super
            .createDelete()
            .onExecute((reactiveDelete, integerMono) ->
                           this.createQuery()
                               .setParam(reactiveDelete.toQueryParam())
                               .fetch()
                               .collectList()
                               .filter(CollectionUtils::isNotEmpty)
                               .flatMap(devices -> integerMono
                                   .flatMap((r) -> {
                                       EntityDeletedEvent<DeviceInstanceEntity> event = new EntityDeletedEvent<>(devices, DeviceInstanceEntity.class);
                                       eventPublisher.publishEvent(new GenericsPayloadApplicationEvent<>(this, event, DeviceInstanceEntity.class));
                                       return event
                                           .getAsync()
                                           .then(Flux
                                                     .fromIterable(devices)
                                                     .flatMap(device -> registry
                                                         .unregisterDevice(device.getId())
                                                         .onErrorResume(err -> Mono.empty())
                                                     )
                                                     .then())
                                           .thenReturn(r);
                                   }))
                               .switchIfEmpty(integerMono))
            ;
    }

    //分页查询设备详情列表
    public Mono<PagerResult<DeviceDetail>> queryDeviceDetail(QueryParamEntity entity) {

        return this
            .queryPager(entity)
            .filter(e -> !e.getData().isEmpty())
            .flatMap(result ->
                         convertDeviceInstanceToDetail(result.getData())
                             .collectList()
                             .map(detailList -> PagerResult.of(result.getTotal(), detailList, entity)))
            .defaultIfEmpty(PagerResult.empty());
    }

    //查询设备详情列表
    public Flux<DeviceDetail> queryDeviceDetailList(QueryParamEntity entity) {
        return this
            .query(entity)
            .collectList()
            .flatMapMany(this::convertDeviceInstanceToDetail);
    }

    private Flux<DeviceDetail> convertDeviceInstanceToDetail(List<DeviceInstanceEntity> instanceList) {
        if (CollectionUtils.isEmpty(instanceList)) {
            return Flux.empty();
        }
        List<String> deviceIdList = new ArrayList<>();
        //按设备产品分组
        Map<String, List<DeviceInstanceEntity>> productGroup = instanceList
            .stream()
            .peek(device -> deviceIdList.add(device.getId()))
            .collect(Collectors.groupingBy(DeviceInstanceEntity::getProductId));
        return Mono
            .zip(
                //T1:查询出所有设备的产品信息
                deviceProductService
                    .findById(productGroup.keySet())
                    .collect(Collectors.toMap(DeviceProductEntity::getId, Function.identity()))
                ,
                //T2:查询出标签并按设备ID分组
                tagRepository
                    .createQuery()
                    .where()
                    .in(DeviceTagEntity::getDeviceId, deviceIdList)
                    .fetch()
                    .collect(Collectors.groupingBy(DeviceTagEntity::getDeviceId))
                    .defaultIfEmpty(Collections.emptyMap())
                ,
                //T3:查询出第三方绑定信息并按设备ID分组
                bindService
                    .createQuery()
                    .where()
                    .in(DeviceBindEntity::getDeviceId, deviceIdList)
                    .fetch()
                    .collect(Collectors.groupingBy(DeviceBindEntity::getDeviceId))
                    .defaultIfEmpty(Collections.emptyMap())
            )
            .flatMapMany(tp3 -> Flux
                //遍历设备,将设备信息转为详情.
                .fromIterable(instanceList)
                .flatMap(instance -> this
                    .createDeviceDetail(
                        // 设备
                        instance
                        //产品
                        , tp3.getT1().get(instance.getProductId())
                        //标签
                        , tp3.getT2().get(instance.getId())
                        //绑定信息
                        , tp3.getT3().get(instance.getId())
                    )
                ))
            //createDeviceDetail是异步操作,可能导致顺序错乱.进行重新排序.
            .sort(Comparator.comparingInt(detail -> deviceIdList.indexOf(detail.getId())))
            ;
    }

    private Mono<DeviceDetail> createDeviceDetail(DeviceInstanceEntity device,
                                                  DeviceProductEntity product,
                                                  List<DeviceTagEntity> tags,
                                                  List<DeviceBindEntity> binds) {

        DeviceDetail detail = new DeviceDetail().with(product).with(device).with(tags).withBind(binds);

        return Mono
            .zip(
                //设备信息
                registry
                    .getDevice(device.getId())
                    //先刷新配置缓存
                    .flatMap(operator -> operator.refreshAllConfig().thenReturn(operator))
                    .flatMap(operator -> operator
                        //检查设备的真实状态,可能出现设备已经离线,但是数据库状态未及时更新的.
                        .checkState()
                        .map(DeviceState::of)
                        //检查失败,则返回原始状态
                        .onErrorReturn(device.getState())
                        //如果状态不一致,则需要更新数据库中的状态
                        .filter(state -> state != detail.getState())
                        .doOnNext(detail::setState)
                        .flatMap(state -> createUpdate()
                            .set(DeviceInstanceEntity::getState, state)
                            .where(DeviceInstanceEntity::getId, device.getId())
                            .execute())
                        .thenReturn(operator)),
                //配置定义
                metadataManager
                    .getDeviceConfigMetadata(device.getId())
                    .flatMapIterable(ConfigMetadata::getProperties)
                    .collectList(),
                detail::with
            )
            //填充详情信息
            .flatMap(Function.identity())
            .switchIfEmpty(
                Mono.defer(() -> {
                    //如果设备注册中心里没有设备信息,并且数据库里的状态不是未激活.
                    //可能是因为注册中心信息丢失,修改数据库中的状态信息.
                    if (detail.getState() != DeviceState.notActive) {
                        return createUpdate()
                            .set(DeviceInstanceEntity::getState, DeviceState.notActive)
                            .where(DeviceInstanceEntity::getId, detail.getId())
                            .execute()
                            .thenReturn(detail.notActive());
                    }
                    return Mono.just(detail.notActive());
                }).thenReturn(detail))
            .onErrorResume(err -> {
                log.warn("get device detail error", err);
                return Mono.just(detail);
            });
    }

    public Mono<DeviceDetail> getDeviceDetail(String deviceId) {

        return this
            .findById(deviceId)
            .map(Collections::singletonList)
            .flatMapMany(this::convertDeviceInstanceToDetail)
            .next();
    }

    public Mono<DeviceState> getDeviceState(String deviceId) {
        return registry.getDevice(deviceId)
                       .flatMap(DeviceOperator::checkState)
                       .flatMap(state -> {
                           DeviceState deviceState = DeviceState.of(state);
                           return createUpdate()
                               .set(DeviceInstanceEntity::getState, deviceState)
                               .where(DeviceInstanceEntity::getId, deviceId)
                               .execute()
                               .thenReturn(deviceState);
                       })
                       .defaultIfEmpty(DeviceState.notActive);
    }

    //获取设备属性
    @SneakyThrows
    public Mono<Map<String, Object>> readProperty(String deviceId,
                                                  String property) {
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .map(DeviceOperator::messageSender)//发送消息到设备
            .map(sender -> sender.readProperty(property).messageId(deviceId))
            .flatMapMany(ReadPropertyMessageSender::send)
            .flatMap(mapReply(ReadPropertyMessageReply::getProperties))
            .reduceWith(LinkedHashMap::new, (main, map) -> {
                main.putAll(map);
                return main;
            });

    }

    //获取标准设备属性
    @SneakyThrows
    public Mono<DeviceProperty> readAndConvertProperty(String deviceId,
                                                       String property) {
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在或未激活"))
            .flatMap(deviceOperator-> deviceOperator.getMetadata().flatMap(dm ->{
                Optional<PropertyMetadata> pm= dm.getProperty(property);
                String readCmd="";
                if(pm.isPresent()) readCmd = pm.get().getExpands().get("readCmd") + "";
                return deviceOperator
                    .messageSender()
                    .readProperty(property)
                    .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                    .header("readCmd",readCmd)
                    .send()
                    .flatMap(mapReply(ReadPropertyMessageReply::getProperties))
                    .reduceWith(LinkedHashMap::new, (main, map) -> {
                        main.putAll(map);
                        return main;
                    })
                    .flatMap(map -> {
                        Object value = map.get(property);
                        return deviceOperator
                            .getMetadata()
                            .map(deviceMetadata -> DeviceProperty.of(value, deviceMetadata.getPropertyOrNull(property)));
                    });
                }));
    }

    //设置设备属性
    @SneakyThrows
    public Mono<Map<String, Object>> writeProperties(String deviceId,
                                                     Map<String, Object> properties) {

        return registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .flatMap(operator -> operator
                .messageSender()
                .writeProperty()
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .write(properties)
                .validate()
            )
            .flatMapMany(WritePropertyMessageSender::send)
            .flatMap(mapReply(WritePropertyMessageReply::getProperties))
            .reduceWith(LinkedHashMap::new, (main, map) -> {
                main.putAll(map);
                return main;
            });
    }

    //设备功能调用
    @SneakyThrows
    public Flux<?> invokeFunction(String deviceId,
                                  String functionId,
                                  Map<String, Object> properties) {
        return invokeFunction(deviceId, functionId, properties, true);
    }

    //设备功能调用
    @SneakyThrows
    public Flux<?> invokeFunction(String deviceId,
                                  String functionId,
                                  Map<String, Object> properties,
                                  boolean convertReply) {
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .flatMap(operator -> operator
                .messageSender()
                .invokeFunction(functionId)
                .messageId(deviceId)
                .setParameter(properties)
                .validate()
            )
            .flatMapMany(FunctionInvokeMessageSender::send)
            .flatMap(convertReply ? mapReply(FunctionInvokeMessageReply::getOutput) : Mono::just);


    }

    //获取设备所有属性
    @SneakyThrows
    public Mono<Map<String, Object>> readProperties(String deviceId, List<String> properties) {

        return registry.getDevice(deviceId)
                       .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
                       .map(DeviceOperator::messageSender)
                       .flatMapMany((sender) -> sender.readProperty()
                                                      .read(properties)
                                                      .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                                                      .send())
                       .flatMap(mapReply(ReadPropertyMessageReply::getProperties))
                       .reduceWith(LinkedHashMap::new, (main, map) -> {
                           main.putAll(map);
                           return main;
                       });
    }

    private Flux<Tuple2<DeviceSaveDetail, DeviceInstanceEntity>> prepareBatchSave(Flux<Tuple2<DeviceSaveDetail, DeviceInstanceEntity>> deviceStream) {
        return deviceStream
            .doOnNext(device -> {
                if (!StringUtils.hasText(device.getT2().getProductId())) {
                    throw new IllegalArgumentException("[productId]不能为空");
                }
            })
            .collect(Collectors.groupingBy(tp2 -> tp2.getT2().getProductId()))
            .flatMapMany(deviceMap -> {
                Set<String> productId = deviceMap.keySet();
                return deviceProductService
                    .findById(productId)
                    .doOnNext(product -> deviceMap
                        .getOrDefault(product.getId(), Collections.emptyList())
                        .forEach(e -> e.getT2().setProductName(product.getName())))
                    .thenMany(
                        Flux.fromIterable(deviceMap.values())
                            .flatMap(Flux::fromIterable)
                    );
            })
            .doOnNext(device -> {
                device.getT1().prepare();
                if (!StringUtils.hasText(device.getT2().getProductName())) {
                    throw new IllegalArgumentException("产品[" + device.getT2().getProductId() + "]不存在");
                }
            });

    }

    public Mono<Integer> batchSave(Flux<DeviceSaveDetail> deviceStream) {
        return this
            .prepareBatchSave(deviceStream.map(detail -> Tuples.of(detail, detail.toInstance())))
            .collectList()
            .flatMap(list -> Flux
                .fromIterable(list)
                .map(Tuple2::getT1)
                .flatMapIterable(DeviceSaveDetail::getTags)
                .as(tagRepository::save)
                .then(Flux.fromIterable(list)
                          .map(Tuple2::getT2)
                          .as(this::save))
            ).map(SaveResult::getTotal);
    }

    private static <R extends DeviceMessageReply, T> Function<R, Mono<T>> mapReply(Function<R, T> function) {
        return reply -> {
            if (ErrorCode.REQUEST_HANDLING.name().equals(reply.getCode())) {
                throw new DeviceOperationException(ErrorCode.REQUEST_HANDLING, reply.getMessage());
            }
            if (!reply.isSuccess()) {
                if (StringUtils.isEmpty(reply.getMessage())) {
                    throw new BusinessException("reply is error");
                }
                throw new BusinessException(reply.getMessage(), reply.getCode());
            }
            return Mono.justOrEmpty(function.apply(reply));
        };
    }

    /**
     * 批量同步设备状态
     *
     * @param batch 设备状态ID流
     * @param force 是否强制获取设备状态,强制获取会去设备连接到服务器检查设备是否真实在线
     * @return 同步数量
     */
    public Flux<List<DeviceStateInfo>> syncStateBatch(Flux<List<String>> batch, boolean force) {

        return batch
            .concatMap(list -> Flux
                .fromIterable(list)
                .publishOn(Schedulers.parallel())
                .flatMap(id -> registry
                    .getDevice(id)
                    .flatMap(operator -> {
                        Mono<Byte> state = force
                            ? operator
                            .checkState()
                            .onErrorResume(err -> operator.getState())
                            : operator.getState();
                        return Mono.zip(
                            state.defaultIfEmpty(org.jetlinks.core.device.DeviceState.offline),//状态
                            Mono.just(operator.getDeviceId()), //设备id
                            operator
                                .getConfig(DeviceConfigKey.isGatewayDevice)
                                .defaultIfEmpty(false)//是否为网关设备
                        );
                    })
                    //注册中心里不存在设备就认为是未激活.
                    .defaultIfEmpty(Tuples.of(org.jetlinks.core.device.DeviceState.noActive, id, false)))
                .collect(Collectors.groupingBy(Tuple2::getT1))
                .flatMapIterable(Map::entrySet)
                .flatMap(group -> {
                    List<String> deviceIdList = group
                        .getValue()
                        .stream()
                        .map(Tuple3::getT2)
                        .collect(Collectors.toList());
                    DeviceState state = DeviceState.of(group.getKey());
                    return Mono
                        .zip(
                            //批量修改设备状态
                            getRepository()
                                .createUpdate()
                                .set(DeviceInstanceEntity::getState, state)
                                .where()
                                .in(DeviceInstanceEntity::getId, deviceIdList)
                                .execute()
                                .flatMap(res -> LedDeployedEvent.of(findById(deviceIdList)).publish(eventPublisher))
                                .thenReturn(group.getValue().size()),
                            //修改子设备状态
                            Flux.fromIterable(group.getValue())
                                .filter(Tuple3::getT3)
                                .map(Tuple3::getT2)
                                .collectList()
                                .filter(CollectionUtils::isNotEmpty)
                                .flatMap(parents ->
                                             getRepository()
                                                 .createUpdate()
                                                 .set(DeviceInstanceEntity::getState, state)
                                                 .where()
                                                 .in(DeviceInstanceEntity::getParentId, parents)
                                                 .execute())
                                .defaultIfEmpty(0))
                        .thenReturn(deviceIdList
                                        .stream()
                                        .map(id -> DeviceStateInfo.of(id, state))
                                        .collect(Collectors.toList()));
                }));
    }


    public Mono<Void> mergeMetadata(String deviceId, DeviceMetadata metadata, MergeOption... options) {

        return Mono
            .zip(this.findById(deviceId)
                     .flatMap(device -> {
                         if (StringUtils.hasText(device.getDeriveMetadata())) {
                             return Mono.just(device.getDeriveMetadata());
                         } else {
                             return deviceProductService
                                 .findById(device.getProductId())
                                 .map(DeviceProductEntity::getMetadata);
                         }
                     })
                     .flatMap(JetLinksDeviceMetadataCodec.getInstance()::decode),
                 Mono.just(metadata),
                 (older, newer) -> older.merge(newer, options)
            )
            .flatMap(JetLinksDeviceMetadataCodec.getInstance()::encode)
            .flatMap(newMetadata -> createUpdate()
                .set(DeviceInstanceEntity::getDeriveMetadata, newMetadata)
                .where(DeviceInstanceEntity::getId, deviceId)
                .execute()
                .then(
                    registry
                        .getDevice(deviceId)
                        .flatMap(device -> device.updateMetadata(newMetadata))
                ))
            .then();
    }


}
