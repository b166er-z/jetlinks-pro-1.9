package org.jetlinks.pro.gateway.external.mqtt;

import io.vertx.mqtt.MqttAuth;
import org.hswebframework.web.authorization.Authentication;
import reactor.core.publisher.Mono;

public interface MqttAuthenticationHandler {

    Mono<Authentication> handle(String clientId,MqttAuth auth);

}
