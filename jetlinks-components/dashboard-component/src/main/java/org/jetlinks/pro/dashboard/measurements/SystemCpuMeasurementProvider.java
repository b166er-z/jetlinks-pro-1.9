package org.jetlinks.pro.dashboard.measurements;

import org.hswebframework.utils.time.DateFormatter;
import org.jetlinks.pro.dashboard.*;
import org.jetlinks.pro.dashboard.supports.StaticMeasurement;
import org.jetlinks.pro.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.unit.UnifyUnit;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;

import static java.math.BigDecimal.ROUND_HALF_UP;

/**
 * 实时CPU 使用率监控
 * <pre>
 *     /dashboard/systemMonitor/cpu/usage/realTime
 * </pre>
 *
 * @author zhouhao
 */
@Component
public class SystemCpuMeasurementProvider
    extends StaticMeasurementProvider {

    public SystemCpuMeasurementProvider() {
        super(DefaultDashboardDefinition.systemMonitor, MonitorObjectDefinition.cpu);
        addMeasurement(cpuUseAgeMeasurement);
    }

    static DataType type = new DoubleType().scale(1).min(0).max(100).unit(UnifyUnit.percent);

    static StaticMeasurement cpuUseAgeMeasurement = new StaticMeasurement(CommonMeasurementDefinition.usage)
        .addDimension(new CpuRealTimeMeasurementDimension());


    static class CpuRealTimeMeasurementDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.realTime;
        }

        @Override
        public DataType getValueType() {
            return type;
        }

        @Override
        public ConfigMetadata getParams() {
            return null;
        }

        @Override
        public boolean isRealTime() {
            return true;
        }

        @Override
        public Flux<MeasurementValue> getValue(MeasurementParameter parameter) {
            //每秒获取系统CPU使用率
            return Flux.interval(Duration.ofSeconds(1))
                .map(t -> SimpleMeasurementValue.of(BigDecimal
                        .valueOf(SystemMonitor.systemCpuUsage.getValue())
                        .setScale(1, ROUND_HALF_UP),
                    DateFormatter.toString(new Date(), "HH:mm:ss"),
                    System.currentTimeMillis()))
                .cast(MeasurementValue.class);
        }

    }

}
