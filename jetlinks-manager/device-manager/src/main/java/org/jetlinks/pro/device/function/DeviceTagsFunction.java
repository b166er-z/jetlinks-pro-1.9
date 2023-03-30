package org.jetlinks.pro.device.function;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.pro.device.entity.DeviceTagEntity;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 获取设备标签函数
 * <p>
 * select device.tags(deviceId,'tag1','tag2')
 *
 * @since 1.3
 */
@Component
public class DeviceTagsFunction extends FunctionMapFeature {

    public DeviceTagsFunction(ReactiveRepository<DeviceTagEntity, String> tagReposiotry) {
        super("device.tags", 100, 1, args ->
            args.collectList()
                .flatMap(list -> {
                    Object deviceId = list.get(0);
                    List<Object> tags = list.stream().skip(1).collect(Collectors.toList());
                    //查询标签
                    return tagReposiotry.createQuery()
                        .where(DeviceTagEntity::getDeviceId, deviceId)
                        .when(!CollectionUtils.isEmpty(tags), query -> query.in(DeviceTagEntity::getKey, tags))
                        .fetch()
                        .collectMap(DeviceTagEntity::getKey, DeviceTagEntity::getValue);

                }));
    }
}
