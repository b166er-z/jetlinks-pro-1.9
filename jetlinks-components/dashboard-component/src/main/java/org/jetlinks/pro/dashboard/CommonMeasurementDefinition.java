package org.jetlinks.pro.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.dashboard.MeasurementDefinition;

/**
 * 通用指标定义
 *
 * @author zhouhao
 */
@AllArgsConstructor
@Getter
public enum CommonMeasurementDefinition implements MeasurementDefinition {
    usage("使用率"),
    used("已使用"),
    info("明细"),
    max("最大值"),
    min("最小值"),
    avg("平均值");

    private String name;

    @Override
    public String getId() {
        return name();
    }
}
