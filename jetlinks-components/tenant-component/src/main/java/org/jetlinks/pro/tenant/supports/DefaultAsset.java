package org.jetlinks.pro.tenant.supports;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.tenant.Asset;
import org.jetlinks.pro.tenant.AssetType;

@Getter
@Setter
@AllArgsConstructor
public class DefaultAsset implements Asset {

    private String id;

    private String name;

    private AssetType type;

}
