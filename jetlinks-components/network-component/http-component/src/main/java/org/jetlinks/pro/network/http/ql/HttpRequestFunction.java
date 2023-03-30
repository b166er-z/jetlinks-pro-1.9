package org.jetlinks.pro.network.http.ql;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.http.client.HttpClient;
import org.jetlinks.pro.network.http.executor.HttpRequestMessageCodec;
import org.jetlinks.pro.network.http.executor.HttpResponseMessageCodec;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;

/**
 * 在SQL中支持http请求函数
 *
 * <pre>
 *
 *     select
 *     http.request('clientId',
 *                  'url','http://www.baidu.com',
 *                  'method','POST')
 *     from dual
 * </pre>
 *
 * @author zhouhao
 * @since 1.1
 */
@Component
@Slf4j
public class HttpRequestFunction extends FunctionMapFeature {

    public HttpRequestFunction(NetworkManager networkManager) {
        super("http.request", 20, 2, args ->
            args.collectList()
                .flatMapMany(list -> {
                    String id = String.valueOf(list.remove(0));
                    Map<String, Object> mapArgs = CastUtils.castMap(list, String::valueOf, Function.identity());
                    HttpRequestMessage msg = HttpRequestMessageCodec.fromMap(mapArgs);
                    return networkManager.<HttpClient>getNetwork(DefaultNetworkType.HTTP_CLIENT, id)
                        .flatMapMany(client -> client.request(msg))
                        .map(response ->
                                 HttpResponseMessageCodec.doEncode(response,
                                                                   MediaType.APPLICATION_JSON.includes(response.getContentType())
                                                                       ? PayloadType.JSON : PayloadType.STRING
                                 )
                        );
                })
                .onErrorResume(err -> {
                    log.error(err.getMessage(), err);
                    Map<String, Object> map = new HashMap<>();
                    map.put("status", -1);
                    map.put("error", err.getMessage());
                    return Mono.just(map);
                })
        );
    }

}
