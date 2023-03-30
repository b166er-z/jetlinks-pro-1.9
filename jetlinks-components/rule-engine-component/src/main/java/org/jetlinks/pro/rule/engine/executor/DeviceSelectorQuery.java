package org.jetlinks.pro.rule.engine.executor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DeviceSelectorQuery {

    @Schema(description = "按固定设备ID")
    private List<String> devices;

    @Schema(description = "按指定产品ID查询")
    private List<String> product;

    @Schema(description = "按设备分组查询")
    private InGroup inGroup;

    @Schema(description = "按指定设备所在同一个分组查询")
    private SameGroup sameGroup;

    @Schema(description = "按标签查询")
    private List<Tag> tags;

    @Schema(description = "按状态查询")
    private State state;

    public enum State {
        online,
        offline
    }

    @Getter
    @Setter
    public static class Tag {

        @Schema(name = "标签key")
        private String key;

        @Schema(name = "标签value")
        private String value;

    }

    @Getter
    @Setter
    public static class SameGroup {

        @Schema(name = "是否使用表达式")
        private boolean expression;

        @Schema(name = "设备ID,如果设置了expression为true,则使用表达式获取设备ID.")
        private String deviceId;

    }

    @Getter
    @Setter
    public static class InGroup {

        @Schema(description = "分组ID")
        private List<String> groupIds;

        @Schema(description = "是否包含子分组")
        private boolean includeChildren;

    }

}
