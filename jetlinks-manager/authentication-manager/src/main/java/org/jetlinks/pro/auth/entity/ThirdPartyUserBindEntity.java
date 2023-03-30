package org.jetlinks.pro.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.api.crud.entity.GenericEntity;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 第三方用户绑定实体
 * @author zhouhao
 * @see
 * @since 1.5
 */
@Getter
@Setter
@Table(name = "s_third_party_user_bind", indexes = {
    @Index(name = "idx_thpub_user_id", columnList = "user_id"),
    @Index(name = "idx_thpub_ptu_id", columnList = "provider,user_id,third_party_user_id", unique = true)
})
public class ThirdPartyUserBindEntity extends GenericEntity<String> {

    @Schema(description = "第三方标识")
    @Column(nullable = false, length = 64, updatable = false)
    private String provider;

    @Schema(description = "第三方名称")
    @Column(nullable = false, length = 64)
    private String providerName;

    @Schema(description = "第三方用户ID")
    @Column(nullable = false, length = 64, updatable = false)
    private String thirdPartyUserId;

    @Schema(description = "平台用户ID")
    @Column(nullable = false, length = 64, updatable = false)
    private String userId;

    @Schema(description = "绑定时间")
    @Column(nullable = false, updatable = false)
    private Long bindTime;

    @Column
    @Schema(description = "说明")
    private String description;

    public void generateId() {
        setId(DigestUtils.md5Hex(String.format("%s%s%s", provider, thirdPartyUserId, userId)));
    }
}
