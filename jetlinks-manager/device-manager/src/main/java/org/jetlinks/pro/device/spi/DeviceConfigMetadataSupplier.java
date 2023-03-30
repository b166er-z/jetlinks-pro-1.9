package org.jetlinks.pro.device.spi;

import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadataType;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.entity.DeviceProductEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 设备配置定义提供者,通常用于第三方平台接入时,告诉系统对应的产品或者设备所需要的配置，如：第三方平台需要的密钥等信息
 * 系统在导入设备或者编辑设备时，会根据配置定义进行不同的操作，如选择前端界面，生成导出模版等
 *
 * @author zhouhao
 * @see org.jetlinks.pro.device.service.DeviceConfigMetadataManager
 * @since 1.7.0
 */
public interface DeviceConfigMetadataSupplier {

    /**
     * @see org.jetlinks.pro.device.service.DeviceConfigMetadataManager#getDeviceConfigMetadata(String)
     */
    Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId);

    /**
     * @see org.jetlinks.pro.device.service.DeviceConfigMetadataManager#getDeviceConfigMetadataByProductId(String)
     */
    Flux<ConfigMetadata> getDeviceConfigMetadataByProductId(String productId);

    /**
     * @see org.jetlinks.pro.device.service.DeviceConfigMetadataManager#getProductConfigMetadata(String)
     */
    Flux<ConfigMetadata> getProductConfigMetadata(String productId);

    /**
     * @see org.jetlinks.pro.device.service.DeviceConfigMetadataManager#getMetadataExpandsConfig(String, DeviceMetadataType, String, String)
     */
    default Flux<ConfigMetadata> getMetadataExpandsConfig(String productId,
                                                          DeviceMetadataType metadataType,
                                                          String metadataId,
                                                          String typeId) {
        return Flux.empty();
    }
}
