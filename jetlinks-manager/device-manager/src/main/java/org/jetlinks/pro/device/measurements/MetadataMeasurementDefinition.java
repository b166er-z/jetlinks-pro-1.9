package org.jetlinks.pro.device.measurements;

import lombok.AllArgsConstructor;
import org.jetlinks.core.metadata.Metadata;
import org.jetlinks.pro.dashboard.MeasurementDefinition;

@AllArgsConstructor(staticName = "of")
public class MetadataMeasurementDefinition implements MeasurementDefinition {

    Metadata metadata;

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public String getName() {
        return metadata.getName();
    }
}
