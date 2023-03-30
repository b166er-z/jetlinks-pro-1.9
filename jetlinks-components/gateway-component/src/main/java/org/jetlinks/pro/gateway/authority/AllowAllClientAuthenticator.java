package org.jetlinks.pro.gateway.authority;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetlinks.core.device.AuthenticationRequest;
import reactor.core.publisher.Mono;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AllowAllClientAuthenticator implements ClientAuthenticator {

    static final Mono<TopicAuthority> allMono = Mono.just(TopicAuthority.ALL);

    static final Mono<ClientAuthentication> allAuth = Mono.just((topic) -> allMono);

    public static final AllowAllClientAuthenticator INSTANCE = new AllowAllClientAuthenticator();

    @Override
    public Mono<ClientAuthentication> authorize(AuthenticationRequest request) {
        return allAuth;
    }
}
