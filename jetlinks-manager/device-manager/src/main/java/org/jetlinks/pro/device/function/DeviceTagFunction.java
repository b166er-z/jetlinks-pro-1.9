package org.jetlinks.pro.device.function;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.pro.device.entity.DeviceTagEntity;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;

/**
 * 获取设备标签函数
 * <p>
 * select device.tag(deviceId,'tag1')
 *
 * @since 1.9
 */
@Component
public class DeviceTagFunction extends FunctionMapFeature {

    public DeviceTagFunction(ReactiveRepository<DeviceTagEntity, String> tagReposiotry) {
        super("device.tag", 2, 2, args ->
            args.collectList()
                .flatMap(list -> {
                    Object deviceId = list.get(0);
                    Object tagKey = list.get(1);
                    //查询标签
                    return tagReposiotry
                        .createQuery()
                        .where(DeviceTagEntity::getDeviceId, deviceId)
                        .where(DeviceTagEntity::getKey, tagKey)
                        .fetch()
                        .take(1)
                        .singleOrEmpty()
                        .map(DeviceTagEntity::getValue);

                }));
    }
}
