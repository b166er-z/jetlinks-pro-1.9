package org.jetlinks.pro.gateway.external.socket;

import org.hswebframework.web.authorization.Authentication;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public interface WebSocketAuthenticationHandler {

    Mono<Authentication> handle(WebSocketSession session);

}
