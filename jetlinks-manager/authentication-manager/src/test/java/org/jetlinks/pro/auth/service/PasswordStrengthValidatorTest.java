package org.jetlinks.pro.auth.service;

import org.hswebframework.web.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordStrengthValidatorTest {

    @Test
    void test(){
        PasswordStrengthValidator validator=new PasswordStrengthValidator();
        validator.setLevel(2);

        assertThrows(ValidationException.class,()->validator.validate("123"));

        assertThrows(ValidationException.class,()->validator.validate("12345678"));

        validator.validate("p@ssw0rd");
        validator.validate("中文1234567");


    }

}