package org.jetlinks.pro.device.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.entity.FirmwareUpgradeHistoryEntity;
import org.jetlinks.pro.device.entity.FirmwareUpgradeTaskEntity;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class FirmwareUpgradeTaskService extends GenericReactiveCrudService<FirmwareUpgradeTaskEntity, String> {

    private final LocalDeviceInstanceService instanceService;

    private final FirmwareService firmwareService;

    private final FirmwareUpgradeHistoryService historyService;

    public FirmwareUpgradeTaskService(LocalDeviceInstanceService instanceService,
                                      FirmwareService firmwareService,
                                      FirmwareUpgradeHistoryService historyService) {
        this.instanceService = instanceService;
        this.firmwareService = firmwareService;
        this.historyService = historyService;
    }

    public Mono<Void> deploy(FirmwareUpgradeTaskEntity taskEntity, Publisher<DeviceInstanceEntity> deviceIdStream) {
        return firmwareService
            .findById(taskEntity.getFirmwareId())
            .flatMap(firmware ->
                Flux.from(deviceIdStream)
                    .map(device -> {
                        FirmwareUpgradeHistoryEntity history = FirmwareUpgradeHistoryEntity
                            .newInstance()
                            .device(device)
                            .with(firmware)
                            .with(taskEntity);
                        //taskId+设备ID 做为主键
                        history.setId(DigestUtils.md5Hex(taskEntity.getId() + device.getId()));
                        return history;
                    })
                    .window(200)//缓冲200
                    .flatMap(historyService::save)
                    .then());
    }

    public Mono<Void> deploy(FirmwareUpgradeTaskEntity taskEntity) {
        return deploy(
            taskEntity,
            instanceService
                .createQuery()
                .select("id", "name")
                .where(DeviceInstanceEntity::getProductId, taskEntity.getProductId())
                .fetch()
        );
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
            .flatMap(id -> historyService.createDelete().where(FirmwareUpgradeHistoryEntity::getTaskId, id).execute().thenReturn(id))
            .as(super::deleteById)
            ;
    }
}
