package org.jetlinks.pro.network.http.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.http.DefaultHttpRequestMessage;
import org.jetlinks.pro.network.http.client.HttpClient;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.LambdaTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.function.Function;

@AllArgsConstructor
@Component
public class HttpClientTaskExecutorProvider implements TaskExecutorProvider {

    private final NetworkManager clientManager;

    static {
        HttpRequestMessageCodec.register();
        HttpResponseMessageCodec.register();
    }

    public Function<RuleData, Publisher<?>> createExecutor(ExecutionContext context, HttpClienTaskConfig config) {
        return data -> clientManager.<HttpClient>getNetwork(DefaultNetworkType.HTTP_CLIENT, config.getClientId())
                .flatMapMany(client -> convertMessage(data, config)
                        .flatMap(message -> client.request(message)
                                .map(responseMessage -> RuleDataCodecs.getCodec(HttpResponseMessage.class)
                                        .map(codec -> codec.encode(responseMessage, config.getResponsePayloadType()))
                                        .orElseThrow(() -> new UnsupportedOperationException("unsupported encode message:{}" + responseMessage)))));
    }

    protected Flux<HttpRequestMessage> convertMessage(RuleData message, HttpClienTaskConfig config) {
        return RuleDataCodecs.getCodec(HttpRequestMessage.class)
                .map(codec ->
                        codec.decode(message, config.getRequestPayloadType())
                                .cast(HttpRequestMessage.class)
                                .map(httpRequestMessage -> convertConfig(httpRequestMessage, config))
                )
                .orElseThrow(() -> new UnsupportedOperationException("unsupported decode message:{}" + message));
    }

    private HttpRequestMessage convertConfig(HttpRequestMessage message, HttpClienTaskConfig config) {
        DefaultHttpRequestMessage defaultHttpRequestMessage = new DefaultHttpRequestMessage();
        defaultHttpRequestMessage.setHeaders(Collections.emptyList());
        defaultHttpRequestMessage.setUrl(config.getUri());
        defaultHttpRequestMessage.setMethod(config.getHttpMethod());
        if (!StringUtils.isEmpty(config.getContentType())) {
            defaultHttpRequestMessage.setContentType(MediaType.valueOf(config.getContentType()));
        }
        return new HttpRequestMessageWrapper(message, defaultHttpRequestMessage);
    }

    @Override
    public String getExecutor() {
        return "http-client";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new LambdaTaskExecutor("HTTP Client", context, () -> {
            HttpClienTaskConfig config = FastBeanCopier.copy(context.getJob().getConfiguration(), HttpClienTaskConfig.class);
            config.validate();
            return createExecutor(context, config);
        }));
    }
}
