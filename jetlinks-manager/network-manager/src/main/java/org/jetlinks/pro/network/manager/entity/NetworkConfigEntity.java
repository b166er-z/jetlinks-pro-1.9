package org.jetlinks.pro.network.manager.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkProperties;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.manager.enums.NetworkConfigState;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.JDBCType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@Table(name = "network_config")
public class NetworkConfigEntity extends GenericEntity<String> {

    @Column
    @NotNull(message = "名称不能为空")
    @Schema(description = "名称")
    private String name;

    @Column
    @Schema(description = "说明")
    private String description;

    /**
     * 组件类型
     * @see NetworkType
     * @see org.jetlinks.pro.network.NetworkTypes
     */
    @Column(nullable = false)
    @NotNull(message = "类型不能为空")
    private String type;

    @Column(nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("disabled")
    @Schema(description = "状态")
    private NetworkConfigState state;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @Schema(description = "配置(根据类型不同而不同)")
    private Map<String, Object> configuration;

    @Column
    @DefaultValue("true")
    @Schema(description = "集群是否共享配置")
    private Boolean shareCluster;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @Schema(description = "集群配置")
    private List<Configuration> cluster;

    public Optional<Map<String, Object>> getConfig(String serverId) {
        if ((Boolean.FALSE.equals(shareCluster))
            && CollectionUtils.isNotEmpty(cluster)) {
            return cluster.stream()
                          .filter(conf -> serverId.equals(conf.serverId))
                          .findAny()
                          .map(Configuration::getConfiguration);
        }
        return Optional.of(configuration);
    }

    public NetworkType lookupNetworkType() {
        return NetworkType.lookup(type).orElseGet(() -> NetworkType.of(type));
    }

    public NetworkType getTypeObject(){
        return lookupNetworkType();
    }

    @Getter
    @Setter
    public static class Configuration implements Serializable {
        private String serverId;

        private Map<String, Object> configuration;
    }

    public Optional<NetworkProperties> toNetworkProperties(String serverId) {
        return this
            .getConfig(serverId)
            .map(configuration -> {
                NetworkProperties properties = new NetworkProperties();
                properties.setConfigurations(configuration);
                properties.setEnabled(state == NetworkConfigState.enabled);
                properties.setId(getId());
                properties.setName(name);
                return properties;
            });
    }

    public NetworkProperties toNetworkProperties() {
        NetworkProperties properties = new NetworkProperties();
        properties.setConfigurations(configuration);
        properties.setEnabled(state == NetworkConfigState.enabled);
        properties.setId(getId());
        properties.setName(name);

        return properties;
    }

}
