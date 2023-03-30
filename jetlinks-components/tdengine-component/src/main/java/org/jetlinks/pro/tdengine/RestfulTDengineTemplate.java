package org.jetlinks.pro.tdengine;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@AllArgsConstructor
@Slf4j
public class RestfulTDengineTemplate implements TDengineOperations {

    private final WebClient webClient;

    @Override
    public Mono<Void> execute(String sql) {
        log.trace("execute tdengine sql : {}", sql);
        return this
            .doRequest(sql)
            .doOnNext(result -> checkExecuteResult(sql, result))
            .then();
    }

    @Override
    public Flux<Map<String, Object>> query(String sql) {
        log.trace("execute tdengine sql : {}", sql);
        return this
            .doRequest(sql)
            .doOnNext(result -> checkExecuteResult(sql, result))
            .flatMapMany(this::convertQueryResult)
            .onErrorResume(TDengineException.class, err -> {
                if (err.getMessage().contains("does not exist")) {
                    return Mono.empty();
                } else {
                    return Mono.error(err);
                }
            });
    }

    private void checkExecuteResult(String sql, JSONObject result) {
        if (!"succ".equals(result.getString("status"))) {
            String error = result.getString("desc");
            if (sql.startsWith("describe") && error.contains("does not exist")) {
                return;
            }
            log.warn("execute tdengine sql error [{}]: [{}]", error, sql);
            throw new TDengineException(sql, result.getString("desc"));
        }
    }

    protected Flux<Map<String, Object>> convertQueryResult(JSONObject result) {

        JSONArray head = result.getJSONArray("head");
        JSONArray data = result.getJSONArray("data");

        if (CollectionUtils.isEmpty(head) || CollectionUtils.isEmpty(data)) {
            return Flux.empty();
        }
        return Flux
            .fromIterable(data)
            .cast(JSONArray.class)
            .map(arr -> {
                Map<String, Object> row = new CaseInsensitiveMap<>();
                for (int i = 0, len = arr.size(); i < len; i++) {
                    String property = head.getString(i);
                    Object value = arr.get(i);
                    row.put(property, value);
                }
                return row;
            });

    }

    private Mono<JSONObject> doRequest(String sql) {
        return webClient
            .post()
            .uri("/rest/sql")
            .bodyValue(sql)
            .exchange()
            .flatMap(response -> response
                .bodyToMono(String.class)
                .map(JSON::parseObject))
            ;
    }
}
