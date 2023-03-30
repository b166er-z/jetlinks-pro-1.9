package org.jetlinks.pro.openapi.manager.service;

import org.hswebframework.web.authorization.ReactiveAuthenticationManager;
import org.jetlinks.pro.openapi.OpenApiClient;
import org.jetlinks.pro.openapi.OpenApiClientManager;
import org.jetlinks.pro.openapi.Signature;
import org.jetlinks.pro.openapi.manager.entity.OpenApiClientEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Service
public class LocalOpenApiClientManager implements OpenApiClientManager {

    @Autowired
    private LocalOpenApiClientService openApiClientService;

    @Autowired
    private ReactiveAuthenticationManager authenticationManager;

    @Override
    public Mono<OpenApiClient> getClient(String clientId) {
        return openApiClientService.findById(Mono.just(clientId))
                .filter(OpenApiClientEntity::statusIsEnabled)
                .flatMap(client -> authenticationManager.getByUserId(client.getUserId())
                        .map(auth -> {
                            OpenApiClient openApiClient = new OpenApiClient();
                            openApiClient.setAuthentication(auth);
                            openApiClient.setSignature(Signature.valueOf(client.getSignature().toUpperCase()));
                            openApiClient.setSecureKey(client.getSecureKey());
                            if(StringUtils.hasText(client.getIpWhiteList())){
                                openApiClient.setIpWhiteList(Arrays.asList(client.getIpWhiteList().split("[,;\n]")));
                            }
                            openApiClient.setClientId(client.getId());
                            openApiClient.setClientName(client.getClientName());
                            return openApiClient;
                        }));
    }
}
