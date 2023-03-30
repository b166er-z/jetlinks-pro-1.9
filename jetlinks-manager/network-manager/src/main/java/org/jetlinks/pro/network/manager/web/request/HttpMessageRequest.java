package org.jetlinks.pro.network.manager.web.request;

import lombok.*;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HttpMessageRequest {


    //消息体
    private Object payload;

    private String url;

    //请求方法
    private HttpMethod method;

    //请求头
    private List<Header> headers;

    //参数
    private Map<String, String> queryParameters;

    //请求类型
    private String contentType;

    private PayloadType requestPayloadType = PayloadType.JSON;

    private PayloadType responsePayloadType = PayloadType.JSON;


    public static HttpMessageRequest of(HttpRequestMessage message, PayloadType type) {
        HttpMessageRequest request = new HttpMessageRequest();
        if (message.getContentType() != null) {
            request.setContentType(message.getContentType().toString());
        }
        request.setHeaders(message.getHeaders());
        request.setMethod(message.getMethod());
        request.setPayload(type.read(message.getPayload()));
        request.setQueryParameters(message.getQueryParameters());
        request.setUrl(message.getUrl());
        return request;
    }


}
