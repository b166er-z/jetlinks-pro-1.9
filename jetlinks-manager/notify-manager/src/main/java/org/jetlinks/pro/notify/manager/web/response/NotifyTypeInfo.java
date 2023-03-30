package org.jetlinks.pro.notify.manager.web.response;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class NotifyTypeInfo {

    @Parameter(description = "通知类型ID")
    private String id;

    @Parameter(description = "通知类型名称")
    private String name;

    @Parameter(description = "服务商信息")
    private List<ProviderInfo> providerInfos;

}
