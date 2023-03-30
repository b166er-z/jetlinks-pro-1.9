package org.jetlinks.pro.openapi.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.basic.web.GeneratedToken;
import org.hswebframework.web.authorization.basic.web.ReactiveUserTokenGenerator;
import org.hswebframework.web.authorization.basic.web.UserTokenGenerator;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.id.IDGenerator;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/token")
@AllArgsConstructor
@Tag(name = "Token申请接口")
public class OpenApiTokenController {

    private final UserTokenManager userTokenManager;

    private final List<ReactiveUserTokenGenerator> generators;

    /**
     * 申请token,之后可以使用token来发起api请求.
     * <pre>
     *
     * POST /api/v1/token
     * X-Client-Id:
     * X-Sign:
     * X-Timestamp:
     *
     * {
     *    "expires": 7200 // 过期时间,单位秒. -1为不过期.
     * }
     *
     * </pre>
     *
     * @param request 请求
     * @return token
     */
    @PostMapping
    @Authorize(merge = false)
    @Operation(summary = "申请默认token")
    public Mono<String> requestToken(@RequestBody Mono<TokenRequest> request) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> request.flatMap(
                req -> {
                    String token = IDGenerator.MD5.generate();
                    return userTokenManager
                        .signIn(token, "open-api", auth.getUser().getId(), req.getExpires() <= 0 ? -1 : req.getExpires() * 1000)
                        .thenReturn(token);
                }
            ));
    }

    /**
     * 申请指定类型到token,之后可以使用token来发起api请求.
     * <pre>
     *
     * POST /api/v1/token/{type}
     * X-Client-Id:
     * X-Sign:
     * X-Timestamp:
     *
     * {
     *    "expires": 7200 // 过期时间,单位秒. 根据token不同
     * }
     *
     * </pre>
     *
     * @param request 请求
     * @return token
     */
    @PostMapping("/{type}")
    @Authorize(merge = false)
    @Operation(summary = "申请指定类型的token",description = "后端通过实现ReactiveUserTokenGenerator接口来自定义token类型")
    public Mono<Map<String, Object>> requestTokenByType(@RequestBody Mono<TokenRequest> request,
                                                        @PathVariable
                                                        @Parameter(description = "token类型") String type) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> request.flatMap(
                req -> generators
                    .stream()
                    .filter(gen -> gen.getTokenType().equals(type))
                    .findFirst()
                    .map(gen -> {
                        GeneratedToken token = gen.generate(auth);
                        Map<String, Object> response = new HashMap<>(token.getResponse());
                        response.put("expires", token.getTimeout() / 1000);
                        if (StringUtils.hasText(token.getToken())) {
                            response.put("token", token.getToken());
                            return userTokenManager
                                .signIn(token.getToken(), "open-api", auth.getUser().getId(), req.getExpires() <= 0 ? token.getTimeout() : req.getExpires() * 1000)
                                .thenReturn(response);
                        }
                        return Mono.just(response);
                    })
                    .orElseGet(() -> Mono.error(new UnsupportedOperationException("token type:" + type + " unsupported!")))
            ));
    }


}
