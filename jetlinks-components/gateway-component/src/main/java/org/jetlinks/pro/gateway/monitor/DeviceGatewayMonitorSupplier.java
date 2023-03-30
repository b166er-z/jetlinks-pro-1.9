package org.jetlinks.pro.gateway.monitor;

public interface DeviceGatewayMonitorSupplier {
      DeviceGatewayMonitor getDeviceGatewayMonitor(String id, String... tags);

}
