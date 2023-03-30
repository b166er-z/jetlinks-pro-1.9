package org.jetlinks.pro.gateway.authority;

import org.jetlinks.core.device.AuthenticationRequest;
import reactor.core.publisher.Mono;

public interface ClientAuthenticator {

    Mono<ClientAuthentication> authorize(AuthenticationRequest request);

    interface Type{
        String getId();

        String getName();
    }
}
