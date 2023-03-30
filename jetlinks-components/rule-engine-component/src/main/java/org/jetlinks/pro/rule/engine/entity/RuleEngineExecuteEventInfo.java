package org.jetlinks.pro.rule.engine.entity;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.api.scheduler.ScheduleJob;
import org.jetlinks.rule.engine.api.task.ExecutionContext;

import java.util.Map;

/**
 * 规则引擎执行日志
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
public class RuleEngineExecuteEventInfo {

    /**
     * ID
     */
    private String id;

    /**
     * 事件
     * @see org.jetlinks.rule.engine.api.RuleConstants.Event
     */
    private String event;

    /**
     * 创建时间
     */
    private long createTime = System.currentTimeMillis();

    /**
     * 时间
     */
    private long timestamp = System.currentTimeMillis();

    /**
     * 规则实例ID
     * @see ExecutionContext#getInstanceId()
     */
    private String instanceId;

    /**
     * 规则节点ID
     * @see RuleNodeModel#getId()
     * @see ExecutionContext#getJob()
     * @see ScheduleJob#getNodeId()
     */
    private String nodeId;

    /**
     * 规则数据JSON
     * @see org.jetlinks.rule.engine.api.RuleData
     */
    private String ruleData;

    /**
     * 上下文ID
     */
    private String contextId;

    public static RuleEngineExecuteEventInfo of(TopicPayload payload) {
        Map<String, String> vars = payload.getTopicVars("/rule-engine/{instanceId}/{nodeId}/event/{event}");
        RuleEngineExecuteEventInfo info = FastBeanCopier.copy(vars, new RuleEngineExecuteEventInfo());
        JSONObject json = payload.bodyToJson(true);
        info.id = json.getString("id");
        info.contextId = json.getString("contextId");
        info.setRuleData(json.toJSONString());
        return info;
    }
}
