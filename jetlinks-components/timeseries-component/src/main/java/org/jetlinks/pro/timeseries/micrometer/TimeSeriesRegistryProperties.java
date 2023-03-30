package org.jetlinks.pro.timeseries.micrometer;

import io.micrometer.core.instrument.step.StepRegistryConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TimeSeriesRegistryProperties extends StepRegistryProperties {

    private List<String> customTagKeys=new ArrayList<>();
}
