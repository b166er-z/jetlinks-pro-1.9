package org.jetlinks.pro.auth.sso;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.auth.sso.oauth2.CommonOAuth2SsoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 单点登录配置
 * <p>
 * 如：
 * <pre>
 *      sso:
 *          token-set-page-url: http://localhost:9000/jetlinks/token-set.html
 *          bind-page-url: http://localhost:9000/#/user/login
 *          base-url: http://localhost:9000/jetlinks
 * <pre/>
 * </p>
 * @author zhouhao
 * @see
 * @since 1.5
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sso")
public class SsoProperties {

    private String tokenSetPageUrl;

    private String bindPageUrl;

    private String baseUrl;

    //通用Oauth2配置
    private List<CommonOAuth2SsoProvider> oauth2 = new ArrayList<>();

    @Autowired
    private WebClient.Builder builder;

    @PostConstruct
    public void init(){
        for (CommonOAuth2SsoProvider provider : oauth2) {
            provider.init(builder);
        }
    }
}
