package org.jetlinks.pro.tdengine;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class RestfulTDengineTemplateTest {


    @Test
    void test() {
        RestfulTDengineTemplate template = new RestfulTDengineTemplate(WebClient.builder().baseUrl("http://localhost:6041/")
            .defaultHeaders(headers -> {
                headers.setBasicAuth("root", "taosdata");
            }).build()
        );

        template
            .execute("CREATE TABLE IF NOT EXISTS jetlinks.test(_ts timestamp,data float)")
            .as(StepVerifier::create)
            .verifyComplete();


    }

}