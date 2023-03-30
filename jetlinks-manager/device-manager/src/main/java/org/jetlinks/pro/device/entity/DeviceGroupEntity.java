package org.jetlinks.pro.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.validator.CreateGroup;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;
import java.util.List;

@Table(name = "dev_device_group", indexes = @Index(
    name = "idx_dev_group_path", columnList = "path"
))
@Getter
@Setter
public class DeviceGroupEntity extends GenericTreeSortSupportEntity<String> {

    @Override
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", message = "ID只能由数字,字母,下划线和中划线组成", groups = CreateGroup.class)
    @Length(max = 32, min = 4, message = "ID的长度不能小于4或者大于32", groups = CreateGroup.class)
    public String getId() {
        return super.getId();
    }

    @Column
    @Schema(description = "分组名称")
    private String name;

    @Column
    @Schema(description = "分组说明")
    private String description;

    @Schema(description = "子节点")
    private List<DeviceGroupEntity> children;
}
