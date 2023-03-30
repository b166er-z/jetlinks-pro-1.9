package org.jetlinks.pro.rule.engine.model;

import com.alibaba.fastjson.JSON;
import org.jetlinks.pro.rule.engine.device.SceneRule;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.springframework.stereotype.Component;

@Component
public class SceneRuleModelParser implements RuleModelParserStrategy {
    @Override
    public String getFormat() {
        return "rule-scene";
    }

    @Override
    public RuleModel parse(String modelDefineString) {
        return JSON
            .parseObject(modelDefineString, SceneRule.class)
            .toRule();
    }
}
