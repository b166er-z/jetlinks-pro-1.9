package org.jetlinks.pro.notify.manager.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.jetlinks.pro.notify.NotifierProperties;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Table(name = "notify_config")
@Getter
@Setter
@EnableEntityEvent
public class NotifyConfigEntity extends GenericEntity<String> {
    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * 配置名称
     */
    @Column
    @Schema(description = "配置名称")
    private String name;

    /**
     * 通知类型
     */
    @Column
    @Schema(description = "通知类型")
    private String type;

    /**
     * 服务提供商
     */
    @Column
    @Schema(description = "服务提供商")
    private String provider;

    /**
     * 描述
     */
    @Column
    @Schema(description = "描述")
    private String description;

    @Comment("重试策略,如: [\"1s\",\"20s\",\"5m\",\"15m\"]")
    @Column(length = 512)
    @JsonCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "重试间隔")
    @Hidden//暂未实现
    private List<String> retryPolicy;

    @Column
    @Comment("最大重试次数")
    @DefaultValue("0")
    @Schema(description = "最大重试次数")
    @Hidden//暂未实现
    private Integer maxRetryTimes;

    /**
     * 配置详情
     */
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "配置信息")
    private Map<String, Object> configuration;

    public NotifierProperties toProperties() {
        NotifierProperties properties = new NotifierProperties();
        properties.setProvider(provider);
        properties.setId(getId());
        properties.setType(type);
        properties.setConfiguration(configuration == null ? new HashMap<>() : configuration);
        properties.setName(name);
        return properties;
    }
}
