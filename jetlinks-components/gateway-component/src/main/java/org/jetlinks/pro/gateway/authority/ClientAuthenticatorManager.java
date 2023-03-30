package org.jetlinks.pro.gateway.authority;

import reactor.core.publisher.Mono;

public interface ClientAuthenticatorManager {

    default Mono<ClientAuthenticator> getAuthenticator(ClientAuthenticator.Type type) {
        return getAuthenticator(type.getId());
    }

    Mono<ClientAuthenticator> getAuthenticator(String type);
}
