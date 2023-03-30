package org.jetlinks.pro.tenant.supports;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.TenantAsset;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DefaultTenantAsset implements TenantAsset {

    private String tenantId;

    private String assetId;

    private AssetType assetType;

    private String ownerId;

    private long permissionValue;

}
