package org.jetlinks.pro.network.http.executor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.message.codec.http.HttpExchangeMessage;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * HTTP服务任务执行管理器，用于管理HTTP服务监听与响应
 *
 * @author zhouhao
 * @since 1.3
 */
@Slf4j
@Component
public class HttpServerTaskExecutorManager {

    private final ClusterManager clusterManager;

    private final String queueId;

    private final Cache<String, HttpExchangeMessage> exchangeMessageCache = CacheBuilder.newBuilder()
        .<String, HttpExchangeMessage>removalListener(notification -> {
            if (notification.getCause() != RemovalCause.EXPLICIT) {
                notification.getValue()
                            .error(504, "{\"message\":\"timeout\"}")
                            .subscribe();
            }
        })
        .expireAfterWrite(Duration.ofSeconds(30))
        .build();

    public HttpServerTaskExecutorManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        this.queueId = "__http_listener_:" + clusterManager.getCurrentServerId();

        this.clusterManager.<RuleData>getQueue(this.queueId)
            .subscribe()
            .flatMap(data ->
                         handleResponse(data)
                             .onErrorResume((err) -> {
                                 log.error(err.getMessage(), err);
                                 return Mono.empty();
                             }))
            .subscribe();

        Flux.interval(Duration.ofSeconds(10))
            .subscribe(t -> exchangeMessageCache.cleanUp());

    }

    protected Mono<Void> handleResponse(RuleData ruleData) {

        HttpExchangeMessage exchange = exchangeMessageCache.getIfPresent(ruleData.getContextId());
        if (exchange == null) {
            log.debug("无法响应未等待中的http请求:{}", ruleData);
            return Mono.empty();
        }
        exchangeMessageCache.invalidate(ruleData.getContextId());

        return Mono.justOrEmpty(RuleDataCodecs.getCodec(HttpResponseMessage.class))
                   .flatMapMany(codec -> codec.decode(ruleData))
                   .cast(HttpResponseMessage.class)
                   .flatMap(exchange::response)
                   .then();

    }

    public Mono<Void> doResponse(HttpServerResponseConfig config, RuleData ruleData) {
        String server = ruleData.getHeader("_http_server")
                                .map(String::valueOf)
                                .orElse(null);
        if (server == null) {
            return Mono.error(new UnsupportedOperationException("无法识别的http响应消息"));
        }
        //转换数据
        ruleData.acceptMap(map -> {
            if (!map.containsKey("status")) {
                map.put("status", config.getStatus());
            }
            if (!map.containsKey("headers")) {
                map.put("headers", config.getHeaders());
            }
        });
        //请求和响应的节点在同一台服务器上，则直接响应结果
        if (server.equals(queueId)) {
            return handleResponse(ruleData);
        }
        //使用集群来通知
        return clusterManager
            .getQueue(server)
            .add(Mono.just(ruleData))
            .then();
    }

    public RuleData addWaitResponse(String urlTemplate, RuleData ruleData, HttpExchangeMessage exchange) {
        ruleData.setHeader("_http_server", queueId);
        exchangeMessageCache.put(ruleData.getContextId(), exchange);

        RuleDataCodecs.getCodec(HttpRequestMessage.class)
                      .map(codec -> codec.encode(exchange))
                      .ifPresent(ruleData::setData);

        if (urlTemplate.contains("{")) {
            ruleData.acceptMap(map -> map.put("pathVars", TopicUtils.getPathVariables(urlTemplate, exchange.getUrl())));
        }

        return ruleData;
    }


}
