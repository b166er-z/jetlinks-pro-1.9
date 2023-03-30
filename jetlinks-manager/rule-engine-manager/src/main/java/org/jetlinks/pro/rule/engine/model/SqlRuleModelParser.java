package org.jetlinks.pro.rule.engine.model;

import com.alibaba.fastjson.JSON;
import org.jetlinks.pro.rule.engine.cluster.SchedulerSelectorStrategy;
import org.jetlinks.pro.rule.engine.cluster.strategies.MinimumLoadSchedulerSelectorStrategy;
import org.jetlinks.pro.rule.engine.enums.SqlRuleType;
import org.jetlinks.pro.rule.engine.ql.SqlRule;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.model.RuleLink;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SqlRuleModelParser implements RuleModelParserStrategy {
    @Override
    public String getFormat() {
        return "sql_rule";
    }

    @Override
    public RuleModel parse(String modelDefineString) {

        SqlRule sqlRule = JSON.parseObject(modelDefineString, SqlRule.class);

        sqlRule.validate();

        RuleModel model = new RuleModel();
        model.setId(sqlRule.getId());
        model.setName(sqlRule.getName());

        RuleNodeModel sqlNode = new RuleNodeModel();
        sqlNode.setId("sql");
        sqlNode.setExecutor("reactor-ql");
        sqlNode.setConfiguration(Collections.singletonMap("sql", sqlRule.getSql()));
        sqlNode.setName("SQL");
        if(sqlRule.isGroup()){
            sqlNode.setSchedulingRule(SchedulerSelectorStrategy.minimumLoad());
        }else {
            sqlNode.setSchedulingRule(SchedulerSelectorStrategy.all());
        }
        model.getNodes().add(sqlNode);

        //错误处理
        List<RuleLink> errorHandler = new ArrayList<>();
        if (!CollectionUtils.isEmpty(sqlRule.getWhenErrorThen())) {
            int index = 0;
            for (Action act : sqlRule.getWhenErrorThen()) {
                if (!StringUtils.hasText(act.getExecutor())) {
                    continue;
                }
                index++;
                RuleNodeModel action = new RuleNodeModel();
                action.setId("error:action:" + index);
                action.setName("错误处理:" + index);
                action.setExecutor(act.getExecutor());
                action.setConfiguration(act.getConfiguration());
                RuleLink link = new RuleLink();
                link.setId(action.getId().concat(":").concat(action.getId()));
                link.setName("错误处理:" + index);
                link.setSource(sqlNode);
                link.setType(RuleConstants.Event.error);
                link.setTarget(action);
                errorHandler.add(link);
                model.getNodes().add(action);
            }
        }

        sqlNode.getEvents().addAll(errorHandler);

        //定时触发
        if (sqlRule.getType() == SqlRuleType.timer) {
            RuleNodeModel timerNode = new RuleNodeModel();
            timerNode.setId("timer");
            timerNode.setExecutor("timer");
            timerNode.setName("定时触发");
            timerNode.setConfiguration(Collections.singletonMap("cron", sqlRule.getCron()));
            timerNode.setRuleId(model.getId());
            timerNode.setSchedulingRule(SchedulerSelectorStrategy.minimumLoad());
            RuleLink link = new RuleLink();
            link.setId("sql:timer");
            link.setName("定时触发SQL");
            link.setSource(timerNode);
            link.setTarget(sqlNode);
            timerNode.getOutputs().add(link);
            sqlNode.getInputs().add(link);
            model.getNodes().add(timerNode);
        }


        if (!CollectionUtils.isEmpty(sqlRule.getActions())) {
            int index = 0;
            for (Action operation : sqlRule.getActions()) {
                if (!StringUtils.hasText(operation.getExecutor())) {
                    continue;
                }
                index++;
                RuleNodeModel action = new RuleNodeModel();
                action.setId("action:" + index);
                action.setName("执行动作:" + index);
                action.setExecutor(operation.getExecutor());
                action.setConfiguration(operation.getConfiguration());
                RuleLink link = new RuleLink();
                link.setId(action.getId().concat(":").concat(action.getId()));
                link.setName("执行动作:" + index);
                link.setSource(sqlNode);
                link.setTarget(action);

                action.getInputs().add(link);
                sqlNode.getOutputs().add(link);

                model.getNodes().add(action);

                action.getEvents().addAll(errorHandler);
            }
        }

        return model;
    }
}
