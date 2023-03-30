package org.jetlinks.pro.tenant.impl.entity;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;

@Table(name = "s_member_assets", indexes = {
    @Index(name = "idx_am_tmt", columnList = "tenant_id,user_id,asset_type,asset_id")
})
@Getter
@Setter
public class AssetMemberBindEntity extends GenericEntity<String> {

    //租户ID
    @Column(length = 64, nullable = false, updatable = false)
    private String tenantId;

    //成员ID
    @Column(length = 64, nullable = false, updatable = false)
    private String userId;

    //资产类型
    @Column(length = 32, nullable = false, updatable = false)
    private String assetType;

    //资产ID, 全部资产为
    @Column(length = 64, nullable = false, updatable = false)
    private String assetId;

    @Column(nullable = false)
    @DefaultValue("0")
    private Long permission;

    public String generateId() {
        setId(generateId(tenantId, userId, assetType, assetId));
        return getId();
    }

    public static String generateId(String tenantId,
                                    String userId,
                                    String assetType,
                                    String assetId) {
        return DigestUtils.md5Hex(String.format("%s|%s|%s|%s", tenantId, userId, assetType, assetId));
    }
}
