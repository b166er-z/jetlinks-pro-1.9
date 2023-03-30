package org.jetlinks.pro.network.manager.web.response;

import lombok.*;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gaoyf
 * @author bestfeng
 * @since 1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class HttpMessageResponse {

    private int status;

    private MediaType contentType;

    private List<Header> headers = new ArrayList<>();

    private Object payload;

    public static HttpMessageResponse of(HttpResponseMessage message, PayloadType type) {
        HttpMessageResponse responseMessage = new HttpMessageResponse();
        responseMessage.setContentType(message.getContentType());
        responseMessage.setHeaders(message.getHeaders());
        responseMessage.setPayload(type.read(message.getPayload()));
        responseMessage.setStatus(message.getStatus());
        return responseMessage;
    }
}
