package org.jetlinks.pro.auth.captcha;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 验证码配置
 * <p>
 * 如：
 * <pre>
 *      captcha:
 *          enabled: true # 开启验证码
 *          ttl: 2m #验证码过期时间,2分钟
 * <pre/>
 * </p>
 * @author zhouhao
 * @see
 * @since 1.4
 */
@Component
@ConfigurationProperties(prefix = "captcha")
@Getter
@Setter
public class CaptchaProperties {
    //是否开启验证码
    private boolean enabled = false;

    //过期时间
    private Duration ttl = Duration.ofMinutes(2);

    //验证码
    private CaptchaType type = CaptchaType.image;

    public enum CaptchaType {
        image
    }
}
