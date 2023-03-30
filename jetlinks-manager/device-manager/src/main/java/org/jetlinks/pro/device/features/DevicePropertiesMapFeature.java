package org.jetlinks.pro.device.features;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.pro.device.entity.DeviceProperty;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;

/**
 * 获取设备属性信息
 *
 * @author zhouhao
 */
@Component
public class DevicePropertiesMapFeature extends FunctionMapFeature {

    public DevicePropertiesMapFeature(DeviceDataService deviceDataService) {
        super("device.properties", 20, 1, args -> args
            .collectList()
            .flatMap(list -> {
                String deviceId = String.valueOf(list.get(0));
                if (list.size() == 1) {
                    return deviceDataService
                        .queryEachOneProperties(deviceId, QueryParamEntity.of())
                        .collectMap(DeviceProperty::getProperty, DeviceProperty::getValue);
                } else {
                    return deviceDataService
                        .queryEachOneProperties(deviceId, QueryParamEntity.of(), list.stream().skip(1).map(String::valueOf).toArray(String[]::new))
                        .collectMap(DeviceProperty::getProperty, DeviceProperty::getValue);
                }

            }));
    }


}
