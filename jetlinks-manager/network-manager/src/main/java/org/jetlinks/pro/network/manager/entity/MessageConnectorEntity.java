package org.jetlinks.pro.network.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.pro.network.manager.enums.ConnectorState;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

@Table(name = "s_message_connector")
@Getter
@Setter
public class MessageConnectorEntity extends GenericEntity<String> {

    @Column(nullable = false, updatable = false)
    private String gatewayId;

    @Column(nullable = false)
    private String gatewayName;

    @Column(nullable = false, updatable = false)
    private String provider;

    @Column
    private String name;

    @Column(nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("stopped")
    private ConnectorState state;

    @Column
    @JsonCodec
    @ColumnType(javaType = String.class, jdbcType = JDBCType.CLOB)
    private Map<String, Object> configuration;

}
