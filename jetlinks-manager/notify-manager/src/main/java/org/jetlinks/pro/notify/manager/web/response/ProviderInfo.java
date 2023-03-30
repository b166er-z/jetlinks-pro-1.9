package org.jetlinks.pro.notify.manager.web.response;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.notify.NotifierProvider;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@AllArgsConstructor
public class ProviderInfo {

    @Parameter(description = "通知类型")
    private final String type;

    @Parameter(description = "服务商ID")
    private final String id;

    @Parameter(description = "服务商名称")
    private final String name;

    public static ProviderInfo of(NotifierProvider provider) {
        return new ProviderInfo(provider.getType().getId(), provider.getProvider().getId(), provider.getProvider().getName());
    }

}
