package org.jetlinks.pro.notify.sms;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.utils.ExpressionUtils;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.ConfigMetadataConstants;
import org.jetlinks.pro.notify.template.Template;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
@Setter
public class PlainTextSmsTemplate implements Template {

    public static final DefaultConfigMetadata templateConfig = new DefaultConfigMetadata("模版配置", "")
            .add("text", "短信内容", "短信内容,支持使用变量:${ }", new StringType()
                    .expand(ConfigMetadataConstants.maxLength.value(512L)))
            .add("sendTo", "收件人", "", new ArrayType().elementType(new StringType()));

    private String text;

    private List<String> sendTo;

    public String getTextSms(Map<String, Object> context) {
        return ExpressionUtils.analytical(text, context, "spel");
    }

    public String[] getSendTo(Map<String, Object> context) {

        return sendTo.stream()
                .map(str -> ExpressionUtils.analytical(str, context, "spel")).toArray(String[]::new);

    }

}
