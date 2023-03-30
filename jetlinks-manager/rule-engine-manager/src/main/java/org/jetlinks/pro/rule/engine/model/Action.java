package org.jetlinks.pro.rule.engine.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 执行动作
 *
 * @since 1.6
 */
@Getter
@Setter
public class Action implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    @Schema(description = "动作名称")
    private String name;

    /**
     * 执行器
     *
     * @see RuleNodeModel#getExecutor()
     */
    @Schema(description = "规则执行器标识")
    @NotBlank
    private String executor;

    /**
     * 执行器配置
     *
     * @see RuleNodeModel#getConfiguration()
     */
    @Schema(description = "规则执行器配置")
    private Map<String, Object> configuration;


    public RuleNodeModel toRuleNode() {
        RuleNodeModel node = new RuleNodeModel();
        node.setName(name);
        node.setExecutor(executor);
        node.setConfiguration(configuration);
        return node;
    }
}
