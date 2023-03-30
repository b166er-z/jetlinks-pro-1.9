package org.jetlinks.pro.network.manager.debug;

import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.jetlinks.pro.gateway.external.SubscribeRequest;

public class DebugAuthenticationHandler {

    public static void handle(SubscribeRequest request){
        if (!request.getAuthentication().hasPermission("network-config", "save")) {
          throw new AccessDenyException();
        }
    }

}
