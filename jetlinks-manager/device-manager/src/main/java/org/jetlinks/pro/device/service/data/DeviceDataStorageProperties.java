package org.jetlinks.pro.device.service.data;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.utils.MessageTypeMatcher;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "jetlinks.device.storage")
@Getter
@Setter
public class DeviceDataStorageProperties {

    //默认数据存储策略,每个属性为一行数据
    private String defaultPolicy = "default-row";

    //是否保存最新数据到数据库
    private boolean enableLastDataInDb = false;

    private Log log = new Log();

    @Getter
    @Setter
    public static class Log extends MessageTypeMatcher {

    }


}
