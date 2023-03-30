package org.jetlinks.pro.device.features;

import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.springframework.stereotype.Component;

@Component
public class DevicePropertyMaxMapFeature extends DevicePropertyAggMapFeature {

    public DevicePropertyMaxMapFeature(DeviceDataService dataService) {
        super("device.property.max", Aggregation.MAX, dataService::aggregationPropertiesByDevice);
    }

}
