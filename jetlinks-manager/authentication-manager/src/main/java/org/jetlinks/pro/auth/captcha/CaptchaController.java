package org.jetlinks.pro.auth.captcha;

import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.events.AuthorizationDecodeEvent;
import org.hswebframework.web.exception.ValidationException;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 系统登录所需验证码，可在application.yml中配置。
 *
 * @author zhouhao
 * @see CaptchaProperties
 * @see ReactiveRedisOperations
 * @see AuthorizationDecodeEvent
 * @since 1.4
 */
@RestController
@Authorize(ignore = true)
@AllArgsConstructor
@RequestMapping("/authorize/captcha")
@Tag(name = "验证码接口")
public class CaptchaController {

    private final CaptchaProperties properties;

    private final ReactiveRedisOperations<String, String> redis;

    /**
     * 获取CaptchaProperties中的配置并返回CaptchaConfig
     * @return CaptchaConfig
     */
    @GetMapping("/config")
    @Operation(summary = "获取验证码相关配置信息")
    public Mono<CaptchaConfig> createCaptcha() {
        CaptchaConfig captchaConfig = new CaptchaConfig();
        captchaConfig.setEnabled(properties.isEnabled());
        captchaConfig.setType(properties.getType().name());
        return Mono.just(captchaConfig);
    }

    /**
     * 获取验证码图片，并将验证码信息存入redis
     * @param width
     * @param height
     * @return
     */
    @GetMapping("/image")
    @Operation(summary = "获取验证码图片")
    public Mono<CaptchaInfo> createCaptcha(@RequestParam(defaultValue = "130")
                                           @Parameter(description = "宽度,默认130px") int width,
                                           @RequestParam(defaultValue = "40")
                                           @Parameter(description = "高度,默认40px") int height) {
        if (!properties.isEnabled()) {
            return Mono.empty();
        }
        SpecCaptcha captcha = new SpecCaptcha(width, height, 4);
        captcha.setCharType(Captcha.TYPE_DEFAULT);

        String base64 = captcha.toBase64();
        String key = UUID.randomUUID().toString();

        return redis
            .opsForValue()
            .set("captcha:" + key, captcha.text(), properties.getTtl())
            .thenReturn(new CaptchaInfo(key, base64));
    }

    /**
     * 开始授权时将发布该事件，此处订阅后等验证码进行验证
     * @param event
     */
    @EventListener
    public void handleAuthEvent(AuthorizationDecodeEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        String key = event.getParameter("verifyKey").map(String::valueOf).orElseThrow(() -> new ValidationException("验证码错误"));
        String code = event.getParameter("verifyCode").map(String::valueOf).orElseThrow(() -> new ValidationException("验证码错误"));
        String redisKey = "captcha:" + key;
        event.async(
            redis
                .opsForValue()
                .get(redisKey)
                .map(code::equalsIgnoreCase)
                .defaultIfEmpty(false)
                .flatMap(checked -> redis
                    .delete(redisKey)
                    .then(checked ? Mono.empty() : Mono.error(new ValidationException("验证码错误"))))
        );

    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CaptchaInfo {
        @Schema(description = "验证码标识,登录时需要在参数[verifyKey]传入此值.")
        private String key;

        @Schema(description = "图片Base64,以data:image/png;base64,开头")
        private String base64;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CaptchaConfig {
        @Schema(description = "是否开启验证码")
        private boolean enabled;

        @Schema(description = "验证码类型")
        private String type;
    }
}
