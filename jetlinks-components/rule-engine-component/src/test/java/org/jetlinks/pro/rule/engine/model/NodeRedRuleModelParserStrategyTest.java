package org.jetlinks.pro.rule.engine.model;

import lombok.SneakyThrows;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class NodeRedRuleModelParserStrategyTest {

    @Test
    @SneakyThrows
    void test() {
        NodeRedRuleModelParserStrategy strategy = new NodeRedRuleModelParserStrategy();
        String model = StreamUtils.copyToString(new ClassPathResource("flows.json").getInputStream(), StandardCharsets.UTF_8);

        RuleModel ruleModel = strategy.parse(model);

    }

}