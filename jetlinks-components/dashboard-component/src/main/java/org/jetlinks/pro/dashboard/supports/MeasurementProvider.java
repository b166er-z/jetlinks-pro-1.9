package org.jetlinks.pro.dashboard.supports;

import org.jetlinks.pro.dashboard.*;
import org.jetlinks.pro.dashboard.measurements.MonitorObjectDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MeasurementProvider {

    /**
     * @return 仪表定义
     * @see DefaultDashboardDefinition
     */
    DashboardDefinition getDashboardDefinition();

    /**
     * @return 对象定义
     * @see MonitorObjectDefinition
     */
    ObjectDefinition getObjectDefinition();

    /**
     * @return 全部指标
     */
    Flux<Measurement> getMeasurements();

    /**
     * @param id 指标ID {@link Measurement#getDefinition()} {@link MeasurementDefinition#getId()}
     * @return 对应等指标, 不存在则返回 {@link Mono#empty()}
     * @see MeasurementDefinition
     */
    Mono<Measurement> getMeasurement(String id);

}
