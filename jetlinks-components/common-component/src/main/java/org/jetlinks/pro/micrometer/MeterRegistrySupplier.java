package org.jetlinks.pro.micrometer;

import io.micrometer.core.instrument.MeterRegistry;

public interface MeterRegistrySupplier {

    MeterRegistry getMeterRegistry(String metric, String... tagKeys);


}
