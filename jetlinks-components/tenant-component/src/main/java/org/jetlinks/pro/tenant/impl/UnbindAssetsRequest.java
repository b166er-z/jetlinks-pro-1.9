package org.jetlinks.pro.tenant.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.validator.ValidatorUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UnbindAssetsRequest {

    @Schema(description = "用户ID,为null则解绑所有成员的资产.")
    private String userId;

    @NotBlank
    @Schema(description = "资产类型")
    private String assetType;

    @NotNull
    @Schema(description = "资产ID集合")
    private List<String> assetIdList;

    public UnbindAssetsRequest validate() {
        ValidatorUtils.tryValidate(this);

        if (CollectionUtils.isEmpty(assetIdList)) {
            throw new ValidationException("assetIdList", "资产ID不能为空");
        }

        return this;
    }
}
