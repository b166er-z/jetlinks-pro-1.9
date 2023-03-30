package org.jetlinks.pro.auth.sso.oauth2;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.utils.RandomUtil;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.auth.sso.ThirdPartyProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2单点登录支持,实现第三方登录支持
 * {@link org.jetlinks.pro.auth.sso.ThirdPartyProvider}

 * @author zhouhao
 * @see
 * @since 1.5
 */
@Getter
@Setter
public class CommonOAuth2SsoProvider implements ThirdPartyProvider {

    private WebClient webClient;

    private String id;

    private String name;

    private String clientId;

    private String clientSecret;

    private String authorizeUrl;

    private String tokenUrl;

    private String userInfoUrl;

    private String redirectUri;

    private String userIdProperty = "login";

    private String usernameProperty = "name";

    public CommonOAuth2SsoProvider() {

    }

    public void init(WebClient.Builder builder){
        this.webClient = builder.build();
    }

    @Override
    public URI getLoginUrl(@Nullable String redirect) {
        Map<String, String> parameters = new HashMap<>();
        String state = RandomUtil.randomChar(6);
        parameters.put("redirect_uri", redirect);
        parameters.put("client_id", clientId);
        parameters.put("scope", "");
        parameters.put("state", state);
        parameters.put("response_type", "code");
        return URI.create(authorizeUrl + "?" + HttpUtils.createEncodedUrlParams(parameters));
    }

    public Mono<String> requestToken(String code, String state) {
        return webClient
            .post()
            .uri(tokenUrl)
            .body(BodyInserters
                .fromFormData("client_id", clientId)
                .with("client_secret", clientSecret)
                .with("code", code)
                .with("state", state)
                .with("grant_type","authorization_code")
                .with("redirect_uri",redirectUri)
            )
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class)
            .map(json -> JSON.parseObject(json).getString("access_token"));
    }

    public Mono<String> getUserId(String accessToken) {
        return webClient
            .get()
            .uri(userInfoUrl)
            .header(HttpHeaders.AUTHORIZATION, "token " + accessToken)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class)
            .map(userJson -> JSON.parseObject(userJson).getString(userIdProperty));
    }

    @Override
    public Mono<NotifyResult> handleNotify(@Nonnull ValueObject parameter) {
        String code = parameter.getString("code").orElse("");
        String state = parameter.getString("state").orElse("");

        return requestToken(code, state)
            .flatMap(this::getUserId)
            .map(userId -> {
                NotifyResult result = new NotifyResult();
                result.setThirdPartyUserId(userId);
                return result;
            });

    }
}
