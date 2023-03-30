package org.jetlinks.pro.device.web.protocol;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ProtocolInfo {

    @Schema(description = "协议ID")
    private String id;

    @Schema(description = "协议名称")
    private String name;

    public static ProtocolInfo of(ProtocolSupport support) {
        return of(support.getId(), support.getName());
    }
}