package org.jetlinks.pro.network.http.executor;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
public class HttpRequestMessageWrapper implements HttpRequestMessage {

    private HttpRequestMessage first;

    private HttpRequestMessage second;


    @Nonnull
    @Override
    public String getUrl() {
        return second.getUrl();
    }

    @Nonnull
    @Override
    public HttpMethod getMethod() {
        return second.getMethod();
    }

    @Nullable
    @Override
    public MediaType getContentType() {
        return second.getContentType();
    }

    @Nonnull
    @Override
    public List<Header> getHeaders() {
        List<Header> list = new ArrayList<>();
        list.addAll(first.getHeaders());
        list.addAll(second.getHeaders());
        return list;
    }

    @Nullable
    @Override
    public Map<String, String> getQueryParameters() {
        Map<String, String> query = new HashMap<>();
        Optional.ofNullable(first.getQueryParameters())
                .ifPresent(query::putAll);
        Optional.ofNullable(second.getQueryParameters())
                .ifPresent(query::putAll);
        return query;
    }

    @Nullable
    @Override
    public Map<String, String> getRequestParam() {
        Map<String, String> requestParam = new HashMap<>();
        Optional.ofNullable(first.getRequestParam())
                .ifPresent(requestParam::putAll);
        Optional.ofNullable(second.getRequestParam())
                .ifPresent(requestParam::putAll);
        return requestParam;
    }

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        return first.getPayload();
    }
}
