package org.jetlinks.pro.device.service.data;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.Value;
import org.jetlinks.core.cache.Caches;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.pro.device.entity.DeviceEvent;
import org.jetlinks.pro.device.entity.DeviceOperationLogEntity;
import org.jetlinks.pro.device.entity.DeviceProperty;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
@Slf4j
public class DefaultDeviceDataService implements DeviceDataService {

    private final DeviceRegistry deviceRegistry;

    private final Map<String, DeviceDataStoragePolicy> policies = Caches.newCache();

    private final Mono<DeviceDataStoragePolicy> defaultPolicyMono;

    private final DeviceLatestDataService databaseOperator;

    private final DeviceDataStorageProperties properties;

    public DefaultDeviceDataService(DeviceRegistry registry,
                                    DeviceDataStorageProperties storeProperties,
                                    ObjectProvider<DeviceDataStoragePolicy> policies,
                                    DeviceLatestDataService databaseOperator) {
        this.deviceRegistry = registry;
        this.databaseOperator = databaseOperator;
        this.properties = storeProperties;
        for (DeviceDataStoragePolicy policy : policies) {
            this.policies.put(policy.getId(), policy);
        }
        defaultPolicyMono = Mono
            .fromSupplier(() -> this.policies.get(properties.getDefaultPolicy()))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("存储策略[" + storeProperties.getDefaultPolicy() + "]不存在")));
    }

    @Override
    public Mono<Void> registerMetadata(@Nonnull String productId, @Nonnull DeviceMetadata metadata) {
        return this
            .getStoreStrategy(productId)
            .flatMap(policy -> policy.registerMetadata(productId, metadata))
            .then(properties.isEnableLastDataInDb()
                      ? databaseOperator.upgradeMetadata(productId, metadata)
                      : Mono.empty());
    }

    @Override
    public Mono<Void> reloadMetadata(@Nonnull String productId, @Nonnull DeviceMetadata metadata) {
        return this
            .getStoreStrategy(productId)
            .flatMap(policy -> policy.reloadMetadata(productId, metadata))
            .then(properties.isEnableLastDataInDb()
                      ? databaseOperator.reloadMetadata(productId, metadata)
                      : Mono.empty());
    }

    Mono<DeviceDataStoragePolicy> getStoreStrategy(String productId) {

        return deviceRegistry
            .getProduct(productId)
            .flatMap(product -> product
                .getConfig("storePolicy")
                .map(Value::asString)
                .map(conf -> Mono
                    .justOrEmpty(policies.get(conf))
                    .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("存储策略[" + deviceRegistry + "]不存在")))
                ).switchIfEmpty(Mono.just(defaultPolicyMono))
                .flatMap(Function.identity()));
    }

    Mono<DeviceDataStoragePolicy> getDeviceStrategy(String deviceId) {
        return deviceRegistry.getDevice(deviceId)
                             .flatMap(DeviceOperator::getProduct)
                             .map(DeviceProductOperator::getId)
                             .flatMap(this::getStoreStrategy);
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachOneProperties(@Nonnull String deviceId,
                                                       @Nonnull QueryParamEntity query,
                                                       @Nonnull String... properties) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.queryEachOneProperties(deviceId, query, properties));
    }


    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachProperties(@Nonnull String deviceId,
                                                    @Nonnull QueryParamEntity query) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.queryEachProperties(deviceId, query));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryTopProperty(@Nonnull String deviceId,
                                                 @Nonnull AggregationRequest request,
                                                 int numberOfTop,
                                                 @Nonnull String... properties) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.queryTopProperty(deviceId, request, numberOfTop, properties));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryProperty(@Nonnull String deviceId,
                                              @Nonnull QueryParamEntity query,
                                              @Nonnull String... property) {

        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.queryProperty(deviceId, query, property));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryPropertyByProductId(@Nonnull String productId,
                                                         @Nonnull QueryParamEntity query,
                                                         @Nonnull String... property) {
        return this
            .getStoreStrategy(productId)
            .flatMapMany(strategy -> strategy.queryPropertyByProductId(productId, query, property));
    }

    @Override
    public Flux<AggregationData> aggregationPropertiesByProduct(@Nonnull String productId,
                                                                @Nonnull AggregationRequest request,
                                                                @Nonnull DevicePropertyAggregation... properties) {
        return this
            .getStoreStrategy(productId)
            .flatMapMany(strategy -> strategy.aggregationPropertiesByProduct(productId, request.copy(), properties));
    }

    @Override
    public Flux<AggregationData> aggregationPropertiesByDevice(@Nonnull String deviceId,
                                                               @Nonnull AggregationRequest request,
                                                               @Nonnull DevicePropertyAggregation... properties) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.aggregationPropertiesByDevice(deviceId, request.copy(), properties));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull String deviceId,
                                                               @Nonnull String property,
                                                               @Nonnull QueryParamEntity query) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMap(strategy -> strategy.queryPropertyPage(deviceId, property, query))
            .defaultIfEmpty(PagerResult.empty());
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPageByProductId(@Nonnull String productId,
                                                                          @Nonnull String property,
                                                                          @Nonnull QueryParamEntity query) {
        return this
            .getStoreStrategy(productId)
            .flatMap(strategy -> strategy.queryPropertyPageByProductId(productId, property, query))
            .defaultIfEmpty(PagerResult.empty());
    }

    @Override
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceMessageLog(@Nonnull String deviceId, @Nonnull QueryParamEntity query) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMap(strategy -> strategy.queryDeviceMessageLog(deviceId, query))
            .defaultIfEmpty(PagerResult.empty());
    }

    private void tryUpdateLatestProperties(DeviceMessage message) {
        if (!properties.isEnableLastDataInDb()||message.getHeader("ignoreLatest").isPresent()) {
            return;
        }
        databaseOperator.save(message);
    }


    @Nonnull
    @Override
    public Mono<Void> saveDeviceMessage(@Nonnull DeviceMessage message) {
        tryUpdateLatestProperties(message);
        return this
            .getDeviceStrategy(message.getDeviceId())
            .flatMap(strategy -> strategy.saveDeviceMessage(message));
    }

    @Nonnull
    @Override
    public Mono<Void> saveDeviceMessage(@Nonnull Publisher<DeviceMessage> message) {
        return Flux
            .from(message)
            .filter(msg->{
                if (!msg.getHeader(Headers.productId).isPresent()) {
                    log.warn("设备消息错误,请检查deviceId对应的设备是否已注册:{}", msg);
                    return false;
                }
                return true;
            })
            .doOnNext(this::tryUpdateLatestProperties)
            .groupBy(DeviceMessage::getDeviceId, Integer.MAX_VALUE)
            .flatMap(group -> this
                .getDeviceStrategy(group.key())
                .flatMap(policy -> policy.saveDeviceMessage(group)))
            .then();
    }

    @Nonnull
    @Override
    public Flux<DeviceEvent> queryEvent(@Nonnull String deviceId,
                                        @Nonnull String event,
                                        @Nonnull QueryParamEntity query, boolean format) {

        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.queryEvent(deviceId, event, query, format));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceEvent>> queryEventPage(@Nonnull String deviceId,
                                                         @Nonnull String event,
                                                         @Nonnull QueryParamEntity query,
                                                         boolean format) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMap(strategy -> strategy.queryEventPage(deviceId, event, query, format))
            .defaultIfEmpty(PagerResult.empty());
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceEvent>> queryEventPageByProductId(@Nonnull String productId,
                                                                    @Nonnull String event,
                                                                    @Nonnull QueryParamEntity query,
                                                                    boolean format) {
        return this
            .getStoreStrategy(productId)
            .flatMap(strategy -> strategy.queryEventPageByProductId(productId, event, query, format))
            .defaultIfEmpty(PagerResult.empty());
    }
}
