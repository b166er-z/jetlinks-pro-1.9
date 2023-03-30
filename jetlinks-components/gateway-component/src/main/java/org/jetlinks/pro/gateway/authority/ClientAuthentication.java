package org.jetlinks.pro.gateway.authority;

import reactor.core.publisher.Mono;

public interface ClientAuthentication {

    Mono<TopicAuthority> getAuthority(String topic);


}
