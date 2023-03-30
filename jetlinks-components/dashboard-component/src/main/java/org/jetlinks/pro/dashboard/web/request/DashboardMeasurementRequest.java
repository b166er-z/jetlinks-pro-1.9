package org.jetlinks.pro.dashboard.web.request;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.dashboard.Dashboard;
import org.jetlinks.pro.dashboard.DashboardDefinition;
import org.jetlinks.pro.dashboard.DimensionDefinition;
import org.jetlinks.pro.dashboard.MeasurementDefinition;
import org.jetlinks.pro.dashboard.web.response.DashboardMeasurementResponse;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘指标数据请求
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
public class DashboardMeasurementRequest {

    /**
     * 分组
     * @see DashboardMeasurementResponse#getGroup()
     */
    private String group;

    /**
     * 仪表盘,如: device
     * @see Dashboard#getDefinition()
     */
    @NotBlank(message = "[dashboard]不能为空")
    private String dashboard;

    /**
     * 仪表对象,如: device1
     * @see  DashboardDefinition#getId()
     */
    @NotBlank(message = "[object]不能为空")
    private String object;

    /**
     * 指标,如: 属性ID
     * @see  MeasurementDefinition#getId()
     */
    @NotBlank(message = "[measurement]不能为空")
    private String measurement;

    /**
     * 维度
     * @see DimensionDefinition#getId()
     */
    @NotBlank(message = "[dimension]不能为空")
    private String dimension;

    /**
     * 查询参数
     */
    private Map<String, Object> params=new HashMap<>();

    public DashboardMeasurementRequest validate(){
        return ValidatorUtils.tryValidate(this);
    }
}
