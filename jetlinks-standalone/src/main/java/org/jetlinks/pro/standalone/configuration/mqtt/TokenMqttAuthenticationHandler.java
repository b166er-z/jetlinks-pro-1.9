package org.jetlinks.pro.standalone.configuration.mqtt;

import io.vertx.mqtt.MqttAuth;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.AuthenticationManager;
import org.hswebframework.web.authorization.ReactiveAuthenticationManager;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.jetlinks.pro.gateway.external.mqtt.MqttAuthenticationHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class TokenMqttAuthenticationHandler implements MqttAuthenticationHandler {

    private final UserTokenManager tokenManager;

    private final ReactiveAuthenticationManager authenticationManager;

    @Override
    public Mono<Authentication> handle(String clientId,MqttAuth auth) {
        return tokenManager
            .getByToken(clientId)
            .flatMap(token->authenticationManager.getByUserId(token.getUserId()));
    }
}
