package org.jetlinks.pro.network.monitor;

import org.jetlinks.pro.micrometer.MeterRegistryManager;
import org.springframework.stereotype.Component;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Component
public class MicrometerNetMonitorSupplier implements NetMonitorSupplier {

    private final MeterRegistryManager meterRegistryManager;

    public MicrometerNetMonitorSupplier(MeterRegistryManager meterRegistryManager) {
        this.meterRegistryManager = meterRegistryManager;
        NetMonitors.register(this);
    }

    @Override
    public NetMonitor getMonitor(String id, String... tags) {
        return new MicrometerNetMonitor(meterRegistryManager.getMeterRegister("net_monitor","target"),
                id,
                tags);
    }
}
