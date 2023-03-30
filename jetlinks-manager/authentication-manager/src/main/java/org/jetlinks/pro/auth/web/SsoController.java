package org.jetlinks.pro.auth.web;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.events.AuthorizationSuccessEvent;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.auth.entity.ThirdPartyUserBindEntity;
import org.jetlinks.pro.auth.sso.SsoProperties;
import org.jetlinks.pro.auth.sso.ThirdPartyProvider;
import org.jetlinks.pro.auth.sso.oauth2.CommonOAuth2SsoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("/sso")
@Authorize(ignore = true)
@Tag(name = "单点登录")
public class SsoController {

    private final Map<String, ThirdPartyProvider> providers = new HashMap<>();

    private final SsoProperties properties;

    private final ReactiveRedisOperations<String, String> redis;

    private final ReactiveRepository<ThirdPartyUserBindEntity, String> bindRepository;

    private final UserTokenManager userTokenManager;

    public SsoController(SsoProperties properties,
                         ReactiveRedisOperations<String, String> redis,
                         ReactiveRepository<ThirdPartyUserBindEntity, String> bindRepository,
                         UserTokenManager userTokenManager,
                         @Autowired(required = false) List<ThirdPartyProvider> providers) {
        this.properties = properties;
        this.redis = redis;
        this.bindRepository = bindRepository;
        this.userTokenManager = userTokenManager;
        for (CommonOAuth2SsoProvider provider : properties.getOauth2()) {
            this.providers.put(provider.getId(), provider);
        }
        if (!CollectionUtils.isEmpty(providers)) {
            for (ThirdPartyProvider provider : providers) {
                this.providers.put(provider.getId(), provider);
            }
        }
    }

    @Getter
    @Setter
    public static class BindCode {
        private String provider;

        private String providerName;

        private ThirdPartyProvider.NotifyResult result;
    }

    @EventListener
    //处理登录时的bind请求
    public void handleAuthBindEvent(AuthorizationSuccessEvent event) {

        String bindCode = event.getParameter("bindCode").map(String::valueOf).orElse("");
        if (StringUtils.isEmpty(bindCode)) {
            return;
        }
        String redisKey = "sso_bind_code:" + bindCode;
        event.async(
            redis
                .opsForValue()
                .get(redisKey)
                .map(bind -> JSON.parseObject(bind, BindCode.class))
                .flatMap(code -> {
                    //保存绑定关系
                    ThirdPartyUserBindEntity bindEntity = new ThirdPartyUserBindEntity();
                    bindEntity.setBindTime(System.currentTimeMillis());
                    bindEntity.setProvider(code.getProvider());
                    bindEntity.setProviderName(code.getProviderName());
                    bindEntity.setDescription(code.getResult().getDescription());
                    bindEntity.setUserId(event.getAuthentication().getUser().getId());
                    bindEntity.setThirdPartyUserId(code.getResult().getThirdPartyUserId());
                    bindEntity.generateId();
                    return bindRepository.save(bindEntity);
                })
                .then(redis.delete(redisKey))
        );

    }

    @GetMapping("/providers")
    @Operation(summary = "获取支持的SSO服务商标识")
    public Flux<String> getProviders() {
        return Flux.fromIterable(providers.keySet());
    }

    @GetMapping("/{provider}/login")
    @Operation(summary = "跳转第三方登录")
    public Mono<Void> redirectSsoLogin(@PathVariable
                                       @Parameter(description = "SSO服务商标识") String provider,
                                       ServerWebExchange exchange) {
        ThirdPartyProvider partyProvider = providers.get(provider);

        if (partyProvider == null) {
            throw new UnsupportedOperationException("unsupported:" + provider);
        }
        String notifyUrl = properties.getBaseUrl() + "/sso/"+provider+"/notify";

        URI url = partyProvider.getLoginUrl(notifyUrl);

        return Mono.fromRunnable(() -> {
            //重定向到登录地址
            exchange.getResponse().setStatusCode(HttpStatus.FOUND);
            exchange.getResponse().getHeaders().setLocation(url);
        });
    }

    @GetMapping("/{provider}/notify")
    @Operation(summary = "登录结果通知")
    public Mono<Void> handleNotify(@PathVariable
                                   @Parameter(description = "SSO服务商标识") String provider,
                                   ServerWebExchange exchange) {
        ThirdPartyProvider partyProvider = providers.get(provider);

        if (partyProvider == null) {
            throw new UnsupportedOperationException("unsupported:" + provider);
        }
        ValueObject parameters = ValueObject.of(exchange.getRequest().getQueryParams().toSingleValueMap());
        String sourceRedirect = parameters.getString("redirect", properties.getBaseUrl());

        return partyProvider
            .handleNotify(parameters)
            .filter(result -> StringUtils.hasText(result.getThirdPartyUserId()))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("unsupported notify")))
            .flatMap(notifyResult ->
                bindRepository
                    .createQuery()
                    .where(ThirdPartyUserBindEntity::getProvider, provider)
                    .and(ThirdPartyUserBindEntity::getThirdPartyUserId, notifyResult.getThirdPartyUserId())
                    .fetchOne()
                    .map(bind -> {
                        //已经绑定了用户
                        String token = IDGenerator.MD5.generate();
                        return userTokenManager
                            .signIn(token, "sso-" + provider, bind.getUserId(), notifyResult.getExpiresMillis())
                            .then(Mono.<Void>fromRunnable(() -> {
                                String redirect = properties.getTokenSetPageUrl();
                                //重定向
                                URI uri = URI.create(redirect + "?token=" + token + "&redirect=" + sourceRedirect);
                                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                                exchange.getResponse().getHeaders().setLocation(uri);
                            }));
                    })
                    .defaultIfEmpty(Mono.defer(() -> {
                        //未绑定用户,则跳转到绑定界面
                        String bindCode = IDGenerator.MD5.generate();
                        BindCode bindCodeResult = new BindCode();
                        bindCodeResult.setProvider(provider);
                        bindCodeResult.setProviderName(partyProvider.getName());
                        bindCodeResult.setResult(notifyResult);
                        return redis
                            .opsForValue()
                            .set("sso_bind_code:" + bindCode, JSON.toJSONString(bindCodeResult), Duration.ofMinutes(5))
                            .then(Mono.fromRunnable(() -> {
                                String redirect = properties.getBindPageUrl();
                                //重定向
                                URI uri = URI.create(redirect + "?code=" + bindCode + "&redirect=" + sourceRedirect);
                                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                                exchange.getResponse().getHeaders().setLocation(uri);
                            }));
                    }))
                    .flatMap(Function.identity()));

    }

}
