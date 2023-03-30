package org.jetlinks.pro.device.service;

import org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService;
import org.jetlinks.pro.device.entity.FirmwareEntity;
import org.springframework.stereotype.Service;

/**
 * 固件管理服务类,用于对设备固件信息进行增删改查
 *
 * @author zhouhao
 * @see GenericReactiveCacheSupportCrudService
 * @since 1.4
 */
@Service
public class FirmwareService extends GenericReactiveCacheSupportCrudService<FirmwareEntity, String> {

    @Override
    public String getCacheName() {
        return "device-firmware";
    }

}
