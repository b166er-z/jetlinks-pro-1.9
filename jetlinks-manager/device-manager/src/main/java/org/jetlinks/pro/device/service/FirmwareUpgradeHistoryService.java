package org.jetlinks.pro.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.message.firmware.UpgradeFirmwareProgressMessage;
import org.jetlinks.pro.device.entity.FirmwareUpgradeHistoryEntity;
import org.jetlinks.pro.device.enums.FirmwareUpgradeState;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Date;

@Service
@Slf4j
public class FirmwareUpgradeHistoryService extends GenericReactiveCrudService<FirmwareUpgradeHistoryEntity, String> {


    /**
     * 订阅固件更新进度
     * @param message 进度消息
     * @return Mono
     */
    @Subscribe("/device/*/*/firmware/progress")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> updateProgress(UpgradeFirmwareProgressMessage message) {

        return createUpdate()
            //成功
            .when(message.isSuccess(), update ->
                update
                    .set(FirmwareUpgradeHistoryEntity::getProgress, message.getProgress())
                    //完成更新时,设置完成时间,状态以及进度设置为100
                    .when(message.isComplete(), update_ ->
                        update_
                            .set(FirmwareUpgradeHistoryEntity::getCompleteTime, new Date(message.getTimestamp()))
                            .set(FirmwareUpgradeHistoryEntity::getState, FirmwareUpgradeState.success)
                    )
                    .where(FirmwareUpgradeHistoryEntity::getDeviceId, message.getDeviceId())
                    .and(FirmwareUpgradeHistoryEntity::getFirmwareId,message.getFirmwareId())
                    .and(FirmwareUpgradeHistoryEntity::getVersion, message.getVersion())
                    .and(FirmwareUpgradeHistoryEntity::getState, FirmwareUpgradeState.processing)
            )
            //失败
            .when(!message.isSuccess(), update ->
                update
                    .set(FirmwareUpgradeHistoryEntity::getErrorReason, message.getErrorReason())
                    .set(FirmwareUpgradeHistoryEntity::getCompleteTime, new Date(message.getTimestamp()))
                    .set(FirmwareUpgradeHistoryEntity::getState, FirmwareUpgradeState.failed)
                    .where(FirmwareUpgradeHistoryEntity::getDeviceId, message.getDeviceId())
                    .and(FirmwareUpgradeHistoryEntity::getVersion, message.getVersion())
                    .and(FirmwareUpgradeHistoryEntity::getFirmwareId,message.getFirmwareId())
                    .and(FirmwareUpgradeHistoryEntity::getState, FirmwareUpgradeState.processing)
            )
            .execute()
            .then();
    }


}
