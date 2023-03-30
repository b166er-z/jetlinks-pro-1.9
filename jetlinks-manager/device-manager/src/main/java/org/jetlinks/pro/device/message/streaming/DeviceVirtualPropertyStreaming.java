package org.jetlinks.pro.device.message.streaming;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.HeaderKey;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.pro.ConfigMetadataConstants;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.entity.DeviceProductEntity;
import org.jetlinks.pro.device.message.DeviceMessageConnector;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.jetlinks.pro.device.service.LocalDeviceProductService;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.streaming.Streaming;
import org.jetlinks.pro.topic.Topics;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * 设备虚拟属性实时计算处理类
 *
 * @author zhouhao
 * @since 1.9
 */
@Slf4j
@AllArgsConstructor
@Component
public class DeviceVirtualPropertyStreaming implements CommandLineRunner {

    private final ConcurrentMap</*设备ID*/String, Map</*属性ID*/String, Streaming<DeviceMessage, Object, DeviceMessage>>>
        deviceStreaming = new ConcurrentHashMap<>();

    private final DevicePropertyStreamingFactory streamingFactory;

    private final DeviceMessageConnector messageConnector;

    private final LocalDeviceProductService productService;

    private final LocalDeviceInstanceService instanceService;

    public static final HeaderKey<Integer> virtualCount = HeaderKey.of("virtualCount", 0);

    private Mono<DeviceMessage> handleMessage(DeviceMessage message) {

        String deviceId = message.getDeviceId();

        String productKey = "product:" + message.getHeader(Headers.productId).orElse("");
        String deviceKey = "device:" + deviceId;

        Map<String, Streaming<DeviceMessage, Object, DeviceMessage>> propertiesStreaming = deviceStreaming
            .getOrDefault(deviceKey, deviceStreaming.get(productKey));

        int virtualCountValue = getVirtualCount(message);

        if (MapUtils.isNotEmpty(propertiesStreaming)) {
            //虚拟属性计算次数超过10 则不再计算,防止递归
            if (message.getHeader(virtualCount).orElse(0) > 10) {
                return Mono.empty();
            }
            return Flux.fromIterable(propertiesStreaming.entrySet())
                       .flatMap(entry -> Mono
                           .zip(
                               Mono.just(entry.getKey()),
                               entry.getValue().compute(message)
                                    .onErrorResume(err -> {
                                        log.warn("handle message compute error", err);
                                        return Mono.empty();
                                    })
                           ))
                       .collectMap(Tuple2::getT1, Tuple2::getT2)
                       .filter(MapUtils::isNotEmpty)
                       .map(values -> convertDeviceMessage(deviceId, values, virtualCountValue + 1));
        }
        return Mono.empty();
    }

    @Subscribe(topics = {
        "/device/*/*/message/property/report",//属性上报
        "/device/*/*/message/property/read/reply",//读取属性回复
        "/device/*/*/message/property/write/reply"//修改属性回复
    })
    public Mono<Void> handleDeviceMessage(DeviceMessage message) {
        return handleMessage(message)
            .flatMap(this::handleOutput);
    }

    private DeviceMessage convertDeviceMessage(String deviceId, Map<String, Object> values, int count) {
        ReportPropertyMessage message = new ReportPropertyMessage();
        message.setDeviceId(deviceId);
        message.setProperties(values);
        //标记为虚拟属性计算次数
        message.addHeader(virtualCount, count);

        return message;
    }

    private Mono<Void> registerStreaming(
        Function<Boolean, Map<String, Streaming<DeviceMessage, Object, DeviceMessage>>> cacheSupplier, DeviceMetadata metadata) {
        Map<String, Streaming<DeviceMessage, Object, DeviceMessage>> old = cacheSupplier.apply(false);
        Map<String, Streaming<DeviceMessage, Object, DeviceMessage>> readyToRemove = old == null ? new HashMap<>() : new HashMap<>(old);
        return this
            .createStreaming(metadata)
            .doOnNext(tp2 -> cacheSupplier
                .apply(true)
                .compute(tp2.getT1(), (key, older) -> {
                    readyToRemove.remove(key);
                    if (older != null) {
                        //如果和旧规则相同,则忽略
                        if (Objects.equals(tp2.getT2(), older) && !older.isDisposed()) {
                            tp2.getT2().dispose();
                            return older;
                        }
                        older.dispose();
                    }
                    tp2.getT2()
                       .output()
                       .flatMap(this::handleOutput)
                       .subscribe();

                    return tp2.getT2();
                }))
            .then(Mono.fromRunnable(() -> {
                //移除旧的规则
                Map<String, Streaming<DeviceMessage, Object, DeviceMessage>> cache =
                    Optional.ofNullable(cacheSupplier.apply(false))
                            .orElseGet(Collections::emptyMap);

                readyToRemove
                    .forEach((property, streaming) -> {
                        cache.remove(property);
                        streaming.dispose();
                    });

            }));
    }

    /**
     * 获取虚拟属性计算次数,虚拟属性可以互相参与计算,如果计数次数太多,可能是递归计算导致的
     *
     * @param message 消息
     * @return 次数, 默认 0
     */
    private int getVirtualCount(DeviceMessage message) {
        return message
            .getHeader("virtual-count")
            .map(CastUtils::castNumber)
            .map(Number::intValue)
            .orElse(0);
    }

    private Mono<Void> handleOutput(DeviceMessage message) {
        //虚拟属性不记录日志
        message.addHeader(Headers.ignoreLog, true);

        message.addHeaderIfAbsent(virtualCount, 1);
        return messageConnector
            .onMessage(message)
            .onErrorResume(err -> Mono.empty());
    }

    public Flux<Tuple2<String, Streaming<DeviceMessage, Object, DeviceMessage>>> createStreaming(DeviceMetadata metadata) {
        return Flux
            .create(sink -> {
                for (PropertyMetadata property : metadata.getProperties()) {
                    //标记了为虚拟属性
                    if (property
                        .getExpand(ConfigMetadataConstants.virtual.getKey())
                        .map(CastUtils::castBoolean)
                        .orElse(false)) {
                        @SuppressWarnings("all")
                        Map<String, Object> rule = property
                            .getExpand(ConfigMetadataConstants.virtualRule.getKey())
                            .map(conf -> ((Map<String, Object>) conf))
                            .orElse(null);
                        if (rule == null) {
                            continue;
                        }
                        sink.next(Tuples.of(property.getId(), streamingFactory.create(property, rule)));
                    }
                }
                sink.complete();
            });
    }


    private Mono<Void> handleProductRegister(String productId, String metadataStr) {
        return handleRegister("product:" + productId, metadataStr);

    }

    private Mono<Void> handleRegister(String key, String metadataStr) {
        return JetLinksDeviceMetadataCodec
            .getInstance()
            .decode(metadataStr)
            .flatMap(metadata -> this
                .registerStreaming((create) -> create
                    ? deviceStreaming.computeIfAbsent(key, ignore -> new ConcurrentHashMap<>())
                    : deviceStreaming.get(key), metadata)
            )
            .onErrorResume(err -> {
                log.warn("register [{}] virtual property error", key, err);
                return Mono.empty();
            });
    }

    private Mono<Void> handleDeviceRegister(String deviceId, String metadataStr) {

        return handleRegister("device:" + deviceId, metadataStr);
    }

    //订阅产品物模型变更事件
    @Subscribe(value = Topics.allProductMetadataChangedEvent, features = {
        Subscription.Feature.local,
        Subscription.Feature.broker
    })
    public Mono<Void> reloadProductStreaming(String productId) {
        return productService
            .findById(productId)
            .flatMap(product -> handleProductRegister(product.getId(), product.getMetadata()))
            .then();
    }

    //订阅设备物模型变更事件
    //目前仅支持产品物模型中配置规则
//    @Subscribe(value = Topics.allDeviceMetadataChangedEvent, features = {
//        Subscription.Feature.local,
//        Subscription.Feature.broker
//    })
    public Mono<Void> reloadDeviceStreaming(String deviceId) {
        return instanceService
            .createQuery()
            .where(DeviceInstanceEntity::getId, deviceId)
            .fetch()
            .filter(instance -> StringUtils.hasText(instance.getDeriveMetadata()))
            .flatMap(device -> handleDeviceRegister(device.getId(), device.getDeriveMetadata()))
            .then();
    }

    @Override
    public void run(String... args) throws Exception {
        Flux
            .merge(
                productService
                    .createQuery()
                    .where(DeviceProductEntity::getState, 1)
                    .fetch()
                    .flatMap(product -> handleProductRegister(product.getId(), product.getMetadata()))
                    .then(Mono.just(1))
//                        ,
//                        instanceService
//                            .createQuery()
//                            .notEmpty(DeviceInstanceEntity::getDeriveMetadata)
//                            .notNull(DeviceInstanceEntity::getDeriveMetadata)
//                            .fetch()
//                            .flatMap(device -> handleDeviceRegister(device.getId(), device.getDeriveMetadata()))
//                            .then(Mono.just(1))

            )
            .subscribe();
    }
}
