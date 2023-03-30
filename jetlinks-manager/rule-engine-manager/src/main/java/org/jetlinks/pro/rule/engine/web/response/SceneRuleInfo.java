package org.jetlinks.pro.rule.engine.web.response;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.rule.engine.device.SceneRule;
import org.jetlinks.pro.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.pro.rule.engine.enums.RuleInstanceState;


@Getter
@Setter
public class SceneRuleInfo extends SceneRule {

    @Schema(description = "场景状态")
    private RuleInstanceState state;

    public static SceneRuleInfo of(RuleInstanceEntity instance) {

        SceneRuleInfo info = FastBeanCopier.copy(JSON.parseObject(instance.getModelMeta()), new SceneRuleInfo());
        info.setState(instance.getState());
        info.setId(instance.getId());
        return info;
    }
}
