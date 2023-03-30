package org.jetlinks.pro.tenant;

import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;

public interface AssetSupplier {

    List<AssetType> getTypes();

    Flux<? extends Asset> getAssets(AssetType type, Collection<?> assetId);
}
