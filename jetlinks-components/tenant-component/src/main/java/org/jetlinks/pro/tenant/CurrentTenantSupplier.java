package org.jetlinks.pro.tenant;

import org.hswebframework.web.authorization.Authentication;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public interface CurrentTenantSupplier extends Supplier<Mono<TenantMember>> {

    Mono<TenantMember> fromAuth(Authentication auth);

}
