package org.jetlinks.pro.rule.engine.device;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.pro.rule.engine.cluster.SchedulerSelectorStrategy;
import org.jetlinks.pro.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.pro.rule.engine.model.Action;
import org.jetlinks.rule.engine.api.model.RuleLink;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 场景联动规则
 *
 * @author zhouhao
 * @since 1.6
 */
@Getter
@Setter
public class SceneRule implements Serializable {
    private static final long serialVersionUID = -1L;

    public static final String modelType = "rule-scene";

    @Schema(description = "ID")
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", message = "ID只能由数字,字母,下划线和中划线组成", groups = CreateGroup.class)
    @NotBlank
    private String id;

    @Schema(description = "场景名称")
    @NotBlank
    private String name;

    @Schema(description = "触发条件")
    @NotNull(message = "触发条件不能为空")
    private List<SceneRuleTrigger> triggers;

    @Schema(description = "是否并行执行动作")
    private boolean parallel;

    @Schema(description = "执行动作")
    private List<Action> actions;

    @Schema(description = "场景生效时间段")
    private String cron="";

    @Schema(description = "触发器触发模式")
    private InspireMode inspireMode = InspireMode.any;

    public void validate() {
        Assert.hasText(id, "场景ID不能为空");
        Assert.notNull(triggers, "触发条件不能为空");

        triggers.forEach(SceneRuleTrigger::validate);
    }

    public RuleModel toRule() {
        validate();
        RuleModel ruleModel = new RuleModel();
        ruleModel.setId("scene:" + id);
        ruleModel.setName(name);

        //场景节点
        RuleNodeModel sceneNode = new RuleNodeModel();
        sceneNode.setId(id);
        sceneNode.setName(name);
        sceneNode.setExecutor("scene");
        sceneNode.setParallel(parallel);
        Map<String, Object> config = new HashMap<>();
        config.put("id", id);
        config.put("name", name);
        config.put("cron",cron);
        config.put("inspireMode",inspireMode.name());
        sceneNode.setConfiguration(config);

        ruleModel.getNodes().add(sceneNode);

        //触发规则
        {
            int i = 0;
            for (SceneRuleTrigger trigger : triggers) {
                if(trigger.getTrigger()==TriggerType.manual){
                    continue;
                }
                i++;
                RuleNodeModel node = trigger.toRuleNode();
                node.setId("scene:" + trigger.getTrigger() + ":" + i);
                node.setName(trigger.getName());
                //建立连接
                RuleLink link = new RuleLink();
                link.setId(node.getId() + "-" + sceneNode.getId());
                link.setName(node.getName());
                link.setSource(node);
                link.setTarget(sceneNode);

                node.getOutputs().add(link);
                sceneNode.getInputs().add(link);

                ruleModel.getNodes().add(node);
            }
        }
        //执行动作
        {
            if (!CollectionUtils.isEmpty(actions)) {
                RuleNodeModel last = null;
                int i = 0;
                for (Action action : actions) {
                    if(isParallel() && action.getExecutor().equals("delay"))continue;
                    i++;
                    RuleNodeModel node = action.toRuleNode();
                    node.setId("scene-action:" + i);

                    ruleModel.getNodes().add(node);

                    RuleLink link = new RuleLink();
                    if (isParallel() || last == null) {
                        //建立连接
                        link.setId(sceneNode.getId() + "-" + node.getId());
                        link.setName(node.getName());
                        link.setSource(sceneNode);
                        link.setTarget(node);
                        sceneNode.getOutputs().add(link);
                    } else {
                        link.setId(last.getId() + "-" + node.getId());
                        link.setName(node.getName());
                        link.setSource(last);
                        link.setTarget(node);
                        last.getOutputs().add(link);
                    }
                    node.getInputs().add(link);
                    last = node;
                }
            }
        }

        return ruleModel;
    }

    public RuleInstanceEntity toInstance() {
        RuleInstanceEntity instanceEntity = new RuleInstanceEntity();
        if (StringUtils.isEmpty(id)) {
            id = IDGenerator.MD5.generate();
        }
        instanceEntity.setId(id);
        instanceEntity.setModelType(modelType);
        instanceEntity.setModelMeta(JSON.toJSONString(this));
        instanceEntity.setCreateTime(System.currentTimeMillis());
        instanceEntity.setName(name);

        return instanceEntity;
    }

    @Getter
    @Setter
    public static class SceneRuleTrigger implements Serializable {

        private static final long serialVersionUID = -1L;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "条件类型")
        @NotNull
        private TriggerType trigger;

        @Schema(description = "定时触发cron表达式,type为timer时不能为空")
        private String cron;

        @Schema(description = "设备触发,type为device时不能为空")
        private DeviceTrigger device;

        @Schema(description = "场景触发,type为scene时不能为空")
        private SceneTrigger scene;

        @Schema(description = "type为other时不能为空,使用其他规则节点来触发场景")
        private Action other;

        public void validate() {
            if (trigger == TriggerType.timer) {
                Assert.hasText(cron, "cron表达式不能为空");
                try {
                    new CronSequenceGenerator(cron);
                } catch (Exception e) {
                    throw new IllegalArgumentException("cron表达式格式错误", e);
                }
            }

            if (trigger == TriggerType.device) {
                Assert.notNull(device, "设备触发配置不能为空");
                device.validate();
            }

            if (trigger == TriggerType.scene) {
                Assert.notNull(scene, "场景触发配置不能为空");
                scene.validate();
            }

            if (trigger == TriggerType.other) {
                Assert.notNull(other, "其他配置不能为空");
            }
        }

        public RuleNodeModel toRuleNode() {
            validate();
            if (trigger == TriggerType.device) {
                return device.toRuleNode();
            }
            if (trigger == TriggerType.timer) {
                RuleNodeModel node = new RuleNodeModel();
                node.setName("定时触发");
                node.setExecutor("timer");
                node.setConfiguration(Collections.singletonMap("cron", cron));
                node.setSchedulingRule(SchedulerSelectorStrategy.minimumLoad());
                return node;
            }
            if (trigger == TriggerType.other) {
                return other.toRuleNode();
            }
            if (trigger == TriggerType.scene) {
                return scene.toRuleNode();
            }
            throw new UnsupportedOperationException("不支持的类型:" + trigger);
        }
    }

    @Getter
    @Setter
    public static class DeviceTrigger implements Serializable {
        private static final long serialVersionUID = -1L;

        @Schema(description = "防抖配置")
        private ShakeLimit shakeLimit;

        @Schema(description = "设备产品ID")
        @NotBlank
        private String productId;

        @Schema(description = "设备ID")
        private String deviceId;

        @Schema(description = "消息类型")
        @NotNull
        private DeviceAlarmRule.MessageType type;

        //物模型属性或者事件的标识 如: fire_alarm
        @Schema(description = "物模型标识,属性或者事件ID")
        private String modelId;

        //过滤条件
        @Schema(description = "过滤条件")
        private List<DeviceAlarmRule.ConditionFilter> filters;

        public String createReactorQL() {
            String topic = type.getTopic(productId, deviceId, modelId);
            String sql = "select * from \"" + topic + "\"";
            if (!CollectionUtils.isEmpty(filters)) {
                String where = filters
                    .stream()
                    .map(filter -> filter.createExpression(type, false))
                    .collect(Collectors.joining(" and "));
                sql += " where " + where;
            }
            return sql;
        }

        public void validate() {
            Assert.notNull(productId, "设备选择器配置不能为null");
            Assert.notNull(type, "消息类型不能为null");
            if (type == DeviceAlarmRule.MessageType.event) {
                Assert.notNull(modelId, "物模型标识不能为null");
            }
        }

        //转为规则模型
        public RuleNodeModel toRuleNode() {
            RuleNodeModel model = new RuleNodeModel();
            //使用reactor-ql执行数据处理
            model.setExecutor("reactor-ql");
            Map<String, Object> config = new HashMap<>();
            config.put("sql", createReactorQL());
            model.setConfiguration(config);
            return model;
        }
    }

    @Getter
    @Setter
    public static class SceneTrigger implements Serializable {
        private static final long serialVersionUID = -1L;

        @Schema(description = "场景ID")
        @Nonnull
        private List<String> sceneIds;

        public void validate() {
            Assert.notEmpty(sceneIds, "场景ID不能为空");
        }

        public RuleNodeModel toRuleNode() {
            RuleNodeModel ruleNodeModel = new RuleNodeModel();
            ruleNodeModel.setName("根据场景触发");
            ruleNodeModel.setExecutor("reactor-ql");

            String sql = this
                .sceneIds
                .stream()
                .map(id -> "select * from \"/scene/" + id + "\"")
                .collect(Collectors.joining("\nunion all\n", "select * from (", ")"));

            ruleNodeModel.setConfiguration(Collections.singletonMap("sql", sql));

            return ruleNodeModel;
        }
    }

    public enum TriggerType {
        @Schema(description = "手动")
        manual,
        @Schema(description = "定时")
        timer,
        @Schema(description = "设备")
        device,
        @Schema(description = "场景")
        scene,
        @Schema(description = "其他")
        other
    }

    public enum InspireMode{
        @Schema(description = "满足任意条件时触发")
        any("any"),
        @Schema(description = "满足所有条件时触发")
        all("all");

        InspireMode(String str){}

        public int value(){
            return ordinal();
        }
    }
}
