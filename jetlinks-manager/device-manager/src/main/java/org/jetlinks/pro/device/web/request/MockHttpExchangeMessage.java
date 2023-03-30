package org.jetlinks.pro.device.web.request;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpExchangeMessage;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class MockHttpExchangeMessage implements HttpExchangeMessage {

    private String url;

    private HttpMethod method;

    private MediaType contentType;

    private List<Header> headers;

    private Map<String, String> queryParameters;

    private Map<String, String> requestParam;

    private ByteBuf payload;

    @Nonnull
    @Override
    public Mono<Void> response(@Nonnull HttpResponseMessage message) {
        log.debug("mock http response:\n{}", message.print());
        return Mono.empty();
    }


}
