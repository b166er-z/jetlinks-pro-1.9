package org.jetlinks.pro.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.JDBCType;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 固件信息
 *
 * @author zhouhao
 * @since 1.3
 */
@Getter
@Setter
@Table(name = "dev_firmware", indexes = {
    @Index(name = "idx_dev_fir_product_id", columnList = "product_id")
})
public class FirmwareEntity extends GenericEntity<String> {
    private static final long serialVersionUID = -6849794470754667710L;

    //所属产品
    @Column(length = 64, nullable = false)
    @NotBlank(message = "[productId]不能为空", groups = CreateGroup.class)
    @Schema(description = "产品ID")
    private String productId;

    //所属产品名称
    @Column(length = 128)
    @NotBlank(message = "[productName]不能为空", groups = CreateGroup.class)
    @Schema(description = "产品名称")
    private String productName;

    //固件名称
    @Column(length = 32, nullable = false)
    @NotBlank(message = "[name]不能为空", groups = CreateGroup.class)
    @Schema(description = "固件名称")
    private String name;

    //版本
    @Column(length = 32, nullable = false)
    @NotBlank(message = "[version]不能为空", groups = CreateGroup.class)
    @Schema(description = "版本号")
    private String version;

    //版本序号
    @Column(nullable = false)
    @NotNull(message = "[versionOrder]不能为空", groups = CreateGroup.class)
    @Schema(description = "版本序号")
    private Integer versionOrder;

    //URL
    @Column(length = 3000, nullable = false)
    @NotBlank(message = "[url]不能为空", groups = CreateGroup.class)
    @Schema(description = "固件文件地址")
    private String url;

    //签名
    @Column(length = 256, nullable = false)
    @NotBlank(message = "[sign]不能为空", groups = CreateGroup.class)
    @Schema(description = "固件文件签名")
    private String sign;

    //签名方式
    @Column(length = 32, nullable = false)
    @NotBlank(message = "[signMethod]不能为空", groups = CreateGroup.class)
    @Schema(description = "固件文件签名方式,如:MD5,SHA256")
    private String signMethod;

    //大小
    @Column(nullable = false)
    @NotNull(message = "[size]不能为空", groups = CreateGroup.class)
    @Schema(description = "固件文件大小")
    private Long size;

    //创建时间
    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间(只读)")
    private Date createTime;

    //其他配置信息
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @JsonCodec
    @Schema(description = "其他拓展信息")
    private List<Property> properties;

    //说明
    @Column
    @Schema(description = "说明")
    private String description;

    public Map<String, Object> propertiesToMap() {
        return properties == null
            ? new HashMap<>()
            : properties.stream()
            .collect(Collectors.toMap(Property::getId, Property::getValue));
    }

    @Getter
    @Setter
    public static class Property implements Serializable {
        private static final long serialVersionUID = -6849794470754667710L;

        private String id;

        private String name;

        private String value;
    }
}
