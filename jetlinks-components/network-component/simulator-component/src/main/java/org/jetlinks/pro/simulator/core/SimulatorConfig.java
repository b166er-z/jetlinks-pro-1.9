package org.jetlinks.pro.simulator.core;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SimulatorConfig {

    /**
     * 模拟器ID
     */
    private String id;

    /**
     * 类型
     *
     * @see SimulatorProvider#getType()
     */
    private String type;

    /**
     * 网络配置
     */
    private Network network;

    /**
     * 运行配置
     */
    private Runner runner = new Runner();

    /**
     * 监听器配置
     */
    private List<Listener> listeners;


    @Getter
    @Setter
    public static class Runner {
        @Schema(description = "绑定网卡")
        private List<String> binds;

        @Schema(description = "启动总数")
        private int total = 1;

        @Schema(description = "起始下标")
        private int startWith = 0;

        @Schema(description = "并发数量")
        private int batch = 100;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Network {
        private Map<String, Object> configuration = new HashMap<>();

        public Network with(String key, Object value) {
            configuration.put(key, value);

            return this;
        }
    }

    @Getter
    @Setter
    public static class Listener {
        @Schema(description = "监听器ID,同一个模拟器不能重复")
        private String id;

        @Schema(description = "监听器名称")
        private String name;

        @Schema(description = "监听器类型")
        private String type;

        @Schema(description = "监听器配置(类型不同配置不同)")
        private Map<String, Object> configuration = new HashMap<>();

        public Listener id(String id) {
            this.id = id;
            return this;
        }

        public Listener type(String value) {
            this.type = value;
            return this;
        }

        public Listener with(String key, Object value) {
            configuration.put(key, value);

            return this;
        }

    }
}
