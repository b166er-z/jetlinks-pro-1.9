package org.jetlinks.pro.network.manager.web.response;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.apache.commons.codec.binary.Base64;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bestfeng
 * @since 1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class HttpResponseMessage {

    private int status = 200;

    private String contentType = MediaType.APPLICATION_JSON_VALUE;

    private String headers;

    private Object payload = "ok";

    @SneakyThrows
    public HttpMessageResponse getHttpMessageResponse() {
        HttpMessageResponse responseMessage = new HttpMessageResponse();
        if (StringUtils.hasText(headers)) {
            responseMessage.setHeaders(JSON.parseArray(headers)
                .stream()
                .map(o -> JSON.parseObject(o.toString()).toJavaObject(Header.class))
                .collect(Collectors.toList()));
        }
        responseMessage.setContentType(MediaType.valueOf(this.getContentType()));
        responseMessage.setPayload(this.getPayload()==null?"":this.getPayload());
        responseMessage.setStatus(this.getStatus());
        return responseMessage;
    }
}
