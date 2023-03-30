package org.jetlinks.pro.gateway.external.socket;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.authorization.token.UserToken;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class PathTokenAuthenticationHandler implements WebSocketAuthenticationHandler {

    private final UserTokenManager tokenManager;

    @Override
    public Mono<Authentication> handle(WebSocketSession session) {

        String query = session.getHandshakeInfo().getUri().getQuery();
        String token;
        if (StringUtils.hasText(query) && query.contains(":X_Access_Token")) {
            token = HttpUtils.parseEncodedUrlParams(query).get(":X_Access_Token");
        } else if (session.getHandshakeInfo().getHeaders().containsKey("X-Access-Token")) {
            token = session
                .getHandshakeInfo()
                .getHeaders()
                .getFirst("X-Access-Token");
        } else {
            String paths = session.getHandshakeInfo().getUri().getPath();
            String[] path = paths.split("[/]");
            if (path.length == 0) {
                return Mono.empty();
            }
            token = path[path.length - 1];
        }
        if (!StringUtils.hasText(token)) {
            return Mono.empty();
        }
        return tokenManager
            .getByToken(token)
            .map(UserToken::getUserId)
            .flatMap(ReactiveAuthenticationHolder::get);
    }
}
