package org.jetlinks.pro.device.message.streaming;

import org.jetlinks.core.message.DeviceDataManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class DevicePropertyScriptInitializer {

    public DevicePropertyScriptInitializer(ObjectProvider<DeviceDataManager> provider){

        DevicePropertyScript.RECENT_DATA_MANAGER = provider.getIfAvailable();
    }

}
