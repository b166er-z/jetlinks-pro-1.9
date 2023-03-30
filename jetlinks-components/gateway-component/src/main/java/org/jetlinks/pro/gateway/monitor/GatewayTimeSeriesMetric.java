package org.jetlinks.pro.gateway.monitor;

import org.jetlinks.pro.timeseries.TimeSeriesMetric;

public interface GatewayTimeSeriesMetric {

    String deviceGatewayMetric = "device_gateway_monitor";

    /**
     * @return 指标标识
     * @see DeviceGatewayMonitor
     */
    static TimeSeriesMetric deviceGatewayMetric() {
        return TimeSeriesMetric.of(deviceGatewayMetric);
    }


}
