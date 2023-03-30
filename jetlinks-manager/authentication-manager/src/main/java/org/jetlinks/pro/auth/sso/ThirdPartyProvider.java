package org.jetlinks.pro.auth.sso;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.ValueObject;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.net.URI;
import java.time.Duration;

/**
 * 第三方登录支持
 *
 * @author zhouhao
 * @since 1.5.0
 */
public interface ThirdPartyProvider {
    /**
     * @return 唯一标识
     */
    String getId();

    /**
     * @return 标识名称
     */
    String getName();

    /**
     * 获取登录地址
     *
     * @param redirect 重定向地址
     * @return 登录地址
     */
    URI getLoginUrl(@Nullable String redirect);

    /**
     * 处理登录通知
     *
     * @param parameter 参数信息
     * @return 通知结果
     */
    Mono<NotifyResult> handleNotify(@Nonnull ValueObject parameter);

    @Getter
    @Setter
    class NotifyResult implements Serializable {

        private static final long serialVersionUID = -6849794470754667710L;

        /**
         * 第三方平台到用户ID
         */
        @Nonnull
        private String thirdPartyUserId;

        /**
         * 说明
         */
        private String description;

        /**
         * 登录有效期
         */
        private long expiresMillis = Duration.ofHours(1).toMillis();
    }
}
