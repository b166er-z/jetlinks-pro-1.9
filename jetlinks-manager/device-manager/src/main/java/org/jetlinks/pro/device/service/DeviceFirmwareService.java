package org.jetlinks.pro.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.firmware.ReportFirmwareMessage;
import org.jetlinks.core.message.firmware.RequestFirmwareMessage;
import org.jetlinks.core.message.firmware.RequestFirmwareMessageReply;
import org.jetlinks.core.message.firmware.UpgradeFirmwareMessage;
import org.jetlinks.pro.device.entity.DeviceFirmwareInfoEntity;
import org.jetlinks.pro.device.entity.FirmwareUpgradeHistoryEntity;
import org.jetlinks.pro.device.entity.PushFirmwareResponse;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.jetlinks.pro.device.enums.FirmwareUpgradeState.*;

@Service
@Slf4j
public class DeviceFirmwareService extends GenericReactiveCrudService<DeviceFirmwareInfoEntity, String> {

    private final FirmwareUpgradeHistoryService historyService;

    private final FirmwareService firmwareService;

    private final DeviceRegistry deviceRegistry;

    public DeviceFirmwareService(FirmwareUpgradeHistoryService historyService,
                                 FirmwareService firmwareService,
                                 DeviceRegistry deviceRegistry) {
        this.historyService = historyService;
        this.firmwareService = firmwareService;
        this.deviceRegistry = deviceRegistry;
    }

    /**
     * 订阅固件信息上报消息
     *
     * @param message 消息
     * @return Mono
     */
    @Subscribe("/device/*/*/firmware/report")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> syncDeviceFirmwareInfo(ReportFirmwareMessage message) {

        DeviceFirmwareInfoEntity entity = new DeviceFirmwareInfoEntity();
        entity.setId(message.getDeviceId());
        entity.setDeviceName(message.getHeader("deviceName").map(String::valueOf).orElse(message.getDeviceId()));
        entity.setProductId(message.getHeader("productId").map(String::valueOf).orElse("null"));
        entity.setUpdateTime(new Date(message.getTimestamp()));
        entity.setProperties(message.getProperties());
        entity.setVersion(message.getVersion());

        // FIXME: 2020/5/11 应该判断更新时间,如果数据库的记录比上报的新则不更新数据.(等easyorm支持upsert自定义条件后实现)
        return save(Mono.just(entity)).then();
    }

    /**
     * 订阅设备发送来固件拉取请求消息
     *
     * @param message 消息
     * @return Mono
     */
    @Subscribe("/device/*/*/firmware/pull")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> handlePullFirmware(RequestFirmwareMessage message) {

        boolean force = message.getHeader(Headers.force).orElse(false);
        boolean latest = message.getHeader("latest").map(CastUtils::castBoolean).orElse(false);

        return historyService
            .createQuery()
            .where(FirmwareUpgradeHistoryEntity::getDeviceId, message.getDeviceId())
            .and(FirmwareUpgradeHistoryEntity::getVersion, message.getRequestVersion())
            //不是强制拉取则拉取升级中的固件
            .when(!force, query -> query.in(FirmwareUpgradeHistoryEntity::getState, waiting, processing))
            //拉取最新的
            .when(latest, query -> query.orderBy(SortOrder.desc(FirmwareUpgradeHistoryEntity::getVersionOrder)))
            //从头拉取
            .when(!latest, query -> query.orderBy(SortOrder.asc(FirmwareUpgradeHistoryEntity::getVersionOrder)))
            .fetchOne()
            .switchIfEmpty(Mono.defer(() -> {
                log.debug("设备[{}]无固件更新信息", message.getDeviceId());
                RequestFirmwareMessageReply reply = message.newReply();
                reply.setMessageId(Optional
                                       .ofNullable(message.getMessageId())
                                       .orElseGet(IDGenerator.SNOW_FLAKE_STRING::generate));
                reply.setSuccess(false);
                reply.setCode("not_found");
                reply.setDeviceId(message.getDeviceId());
                reply.setMessage("无固件更新");
                reply.addHeader(Headers.async, true);
                return deviceRegistry
                    .getDevice(reply.getDeviceId())
                    .flatMap(operator -> operator
                        .messageSender()
                        .send(reply)
                        .then(Mono.empty())
                    );
            }))
            .zipWhen(his -> firmwareService.findById(his.getFirmwareId()))
            .flatMap(his -> {
                RequestFirmwareMessageReply reply = message.newReply();
                reply.setMessageId(Optional
                                       .ofNullable(message.getMessageId())
                                       .orElseGet(IDGenerator.SNOW_FLAKE_STRING::generate));
                reply.setDeviceId(his.getT1().getDeviceId());
                reply.setSuccess(true);
                reply.setUrl(his.getT2().getUrl());
                reply.setParameters(his.getT2().propertiesToMap());
                reply.setVersion(his.getT2().getVersion());
                reply.setSign(his.getT2().getSign());
                reply.setSignMethod(his.getT2().getSignMethod());
                reply.setSize(his.getT2().getSize());
                reply.addHeader(Headers.async, true);
                reply.setFirmwareId(his.getT2().getId());
                return historyService
                    .createUpdate()
                    .set(FirmwareUpgradeHistoryEntity::getState, processing)
                    .where(FirmwareUpgradeHistoryEntity::getId, his.getT1().getId())
                    .execute()
                    .then(
                        //发送响应消息给设备
                        deviceRegistry
                            .getDevice(reply.getDeviceId())
                            .flatMap(operator -> operator
                                .messageSender()
                                .send(reply)
                                .then()
                            )
                    )
                    ;
            });

    }


    public Flux<PushFirmwareResponse> publishTaskUpgrade(String taskId) {
        return historyService
            .createQuery()
            .where(FirmwareUpgradeHistoryEntity::getTaskId, taskId)
            .and(FirmwareUpgradeHistoryEntity::getState, waiting)
            .fetch()
            .buffer(200)
            .publishOn(Schedulers.single())
            .concatMap(list -> this.publishTaskUpgrade(Flux.just(list)));
    }

    /**
     * 推送固件更新到设备
     *
     * @param historyStream 更新记录流
     * @return 推送结果
     */
    @Transactional
    public Flux<PushFirmwareResponse> publishTaskUpgrade(Flux<List<FirmwareUpgradeHistoryEntity>> historyStream) {

        return historyStream
            .concatMap(list -> Flux
                .fromIterable(list)
                .flatMap(his -> Mono.zip(
                    Mono.just(his),
                    firmwareService.findById(his.getFirmwareId())
                ))
                .flatMap(historyAndFirmware -> {
                    UpgradeFirmwareMessage message = new UpgradeFirmwareMessage();
                    message.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());
                    message.setDeviceId(historyAndFirmware.getT1().getDeviceId());
                    message.setSign(historyAndFirmware.getT2().getSign());
                    message.setSignMethod(historyAndFirmware.getT2().getSignMethod());
                    message.setUrl(historyAndFirmware.getT2().getUrl());
                    message.setVersion(historyAndFirmware.getT2().getVersion());
                    message.setParameters(historyAndFirmware.getT2().propertiesToMap());
                    message.setFirmwareId(historyAndFirmware.getT1().getFirmwareId());
                    message.setSize(historyAndFirmware.getT2().getSize());
                    //异步
                    message.addHeader(Headers.async, true);
                    PushFirmwareResponse response = PushFirmwareResponse.of(
                        message.getDeviceId(),
                        historyAndFirmware.getT1().getId(),
                        historyAndFirmware.getT1().getTaskId()
                    );
                    //发送给设备
                    return deviceRegistry
                        .getDevice(message.getDeviceId())
                        .flatMapMany(operator -> operator
                            .messageSender()
                            .send(Mono.just(message))
                            .map(response::with)
                            .onErrorResume(err -> Mono.just(response.error(err)))
                        )
                        .switchIfEmpty(Mono.fromSupplier(() -> response.error("设备未激活")));
                }))
            .collectList()
            .flatMapMany(resp -> historyService
                .createUpdate()
                .set(FirmwareUpgradeHistoryEntity::getState, processing)
                .where()
                .in(FirmwareUpgradeHistoryEntity::getId, resp
                    .stream()
                    .map(PushFirmwareResponse::getHistoryId)
                    .collect(Collectors.toList())
                ).execute()
                .thenMany(Flux.fromIterable(resp)));
    }


}
