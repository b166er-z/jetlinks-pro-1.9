package org.jetlinks.pro.network.manager.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.pro.network.manager.enums.SimulatorStatus;
import org.jetlinks.pro.simulator.core.SimulatorConfig;
import org.springframework.util.CollectionUtils;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Table(name = "s_simulator")
public class SimulatorEntity extends GenericEntity<String> {

    @Column
    @Schema(description = "名称")
    private String name;

    @Column
    @Schema(description = "说明")
    private String description;

    @Column
    @Schema(description = "网络类型")
    private String networkType;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @JsonCodec
    @Schema(description = "网络配置")
    private Map<String, Object> networkConfiguration;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @JsonCodec
    @Schema(description = "启动配置")
    private SimulatorConfig.Runner runner;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @JsonCodec
    @Schema(description = "监听器配置")
    private List<SimulatorConfig.Listener> listeners;

    @Schema(description = "状态(只读)")
    private SimulatorStatus status = SimulatorStatus.stop;

    public SimulatorConfig toConfig() {
        SimulatorConfig config = new SimulatorConfig();
        config.setId(getId());
        config.setNetwork(new SimulatorConfig.Network(networkConfiguration));
        config.setType(networkType);
        config.setRunner(runner);
        if (!CollectionUtils.isEmpty(listeners)) {
            int idx = 0;
            for (SimulatorConfig.Listener listener : listeners) {
                listener.setId(getId() + "-listener-" + idx);
            }
            config.setListeners(listeners);
        }
        return config;
    }
}
