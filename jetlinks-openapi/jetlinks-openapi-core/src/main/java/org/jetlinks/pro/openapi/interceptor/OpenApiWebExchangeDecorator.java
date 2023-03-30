package org.jetlinks.pro.openapi.interceptor;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;

public class OpenApiWebExchangeDecorator extends ServerWebExchangeDecorator {

    private final ServerHttpRequestDecorator requestDecorator;

    private final ServerHttpResponseDecorator responseDecorator;


    public OpenApiWebExchangeDecorator(ServerWebExchange delegate) {
        super(delegate);
        this.requestDecorator = new OpenApiServerHttpRequestDecorator(delegate.getRequest());
        this.responseDecorator = new OpenApiServerHttpResponseDecorator(delegate.getResponse());
    }

    @Override
    public ServerHttpRequest getRequest() {
        return requestDecorator;
    }

    @Override
    public ServerHttpResponse getResponse() {
        return responseDecorator;
    }


}
