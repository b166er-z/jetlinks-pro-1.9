package org.jetlinks.pro.network.manager.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class NetworkConfigInfoResponse {

    @Schema(description = "网络组件ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "详情")
    public String getDetail() {
        if (StringUtils.hasText(address)) {
            return name + "(" + address + ")";
        }
        return name;
    }
}
