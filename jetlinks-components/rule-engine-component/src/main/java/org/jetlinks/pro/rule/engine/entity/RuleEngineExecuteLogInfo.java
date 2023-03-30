package org.jetlinks.pro.rule.engine.entity;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.api.scheduler.ScheduleJob;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.defaults.LogEvent;

/**
 * 规则执行日志
 *
 * @author bsetfeng
 * @see org.jetlinks.rule.engine.api.Logger
 * @since 1.0
 **/
@Getter
@Setter
public class RuleEngineExecuteLogInfo {

    /**
     * ID
     */
    private String id;

    /**
     * 规则实例ID
     *
     * @see ExecutionContext#getInstanceId()
     */
    private String instanceId;

    /**
     * 规则节点ID
     *
     * @see RuleNodeModel#getId()
     * @see ExecutionContext#getJob()
     * @see ScheduleJob#getNodeId()
     */
    private String nodeId;

    /**
     * 日志级别
     *
     * @see org.jetlinks.rule.engine.api.Logger
     */
    private String level;

    /**
     * 日志内容
     */
    private String message;

    /**
     * 创建时间
     */
    private long createTime = System.currentTimeMillis();

    /**
     * 日志时间
     */
    private long timestamp;

    /**
     * 上下文数据
     */
    private String context;
}
