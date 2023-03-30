package org.jetlinks.pro.auth.service;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.system.authorization.api.PasswordValidator;
import org.hswebframework.web.system.authorization.defaults.service.DefaultAuthorizationSettingService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 密码验证器，用于过滤简单密码.
 *
 * @author zhouhao
 * @see PasswordValidator
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "hsweb.user.password.validator")
@Getter
@Setter
public class PasswordStrengthValidator implements PasswordValidator {
    //数字
    public static final String REG_NUMBER = ".*\\d+.*";

    private String[] regex = {
        ".*\\d+.*", //数字
        ".*[A-Z]+.*", //大写字母
        ".*[a-z]+.*", //小写字母
        ".*[^x00-xff]+.*", //2位字符(中文?)
        ".*[~!@#$%^&*()_+|<>,.?/:;'\\[\\]{}\"]+.*", //特殊符号
    };

    private Set<String> blackList = new HashSet<>();

    private int minLength = 8;

    private int level = 2;

    private String message = "密码必须由数字和字母组成";

    @Override
    public void validate(String password) {
        if (StringUtils.isEmpty(password) || password.length() < minLength) {
            throw new ValidationException("密码长度不能低于" + minLength);
        }
        if (blackList.contains(password)) {
            throw new ValidationException("密码强度不够");
        }
        int _level = 0;
        for (String s : regex) {
            if (password.matches(s)) {
                _level++;
            }
        }
        if (_level < level) {
            throw new ValidationException("密码强度不够");
        }
    }
}
