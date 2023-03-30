package org.jetlinks.pro.rule.engine.executor;

import org.jetlinks.rule.engine.api.RuleData;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ScriptTaskExecutorProviderTest {


    @Test
    public void testJavaScript() {
        ScriptTaskExecutorProvider provider = new ScriptTaskExecutorProvider();

        ScriptTaskExecutorProvider.ScriptConfig config = new ScriptTaskExecutorProvider.ScriptConfig();
        config.setLang("js");
        config.setScript("var ctx = context;\n" +
            "handler.onMessage(function(ruleData){\n" +
            "\n" +
            "//return;\n" +
            "return {status:200, payload:ruleData.data}\n" +
            "})");

        Function<RuleData, Publisher<?>> apply = provider.createExecutor(null, config);

        Duration duration = Flux.range(1, 100000)
            .flatMap(i -> apply.apply(RuleData.create(Collections.singletonMap("test", i))))
            .as(StepVerifier::create)
            .expectNextCount(100000)
            .verifyComplete();

        System.out.println(duration);


    }
}