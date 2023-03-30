package org.jetlinks.pro.rule.engine.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.rpc.RpcServiceFactory;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.pro.elastic.search.service.ElasticSearchService;
import org.jetlinks.pro.rule.engine.log.ElasticSearchRuleEngineLogService;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.scheduler.Scheduler;
import org.jetlinks.rule.engine.api.scheduler.SchedulerSelector;
import org.jetlinks.rule.engine.api.task.ConditionEvaluator;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.api.worker.Worker;
import org.jetlinks.rule.engine.cluster.ClusterRuleEngine;
import org.jetlinks.rule.engine.cluster.ClusterSchedulerRegistry;
import org.jetlinks.rule.engine.cluster.SchedulerRegistry;
import org.jetlinks.rule.engine.cluster.TaskSnapshotRepository;
import org.jetlinks.rule.engine.cluster.scheduler.ClusterLocalScheduler;
import org.jetlinks.rule.engine.cluster.worker.ClusterWorker;
import org.jetlinks.rule.engine.condition.ConditionEvaluatorStrategy;
import org.jetlinks.rule.engine.condition.DefaultConditionEvaluator;
import org.jetlinks.rule.engine.condition.supports.DefaultScriptEvaluator;
import org.jetlinks.rule.engine.condition.supports.ScriptConditionEvaluatorStrategy;
import org.jetlinks.rule.engine.condition.supports.ScriptEvaluator;
import org.jetlinks.rule.engine.model.DefaultRuleModelParser;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.jetlinks.rule.engine.model.antv.AntVG6RuleModelParserStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RuleEngineProperties.class)
@Slf4j
public class RuleEngineConfiguration {

    @Bean
    public DefaultRuleModelParser defaultRuleModelParser() {
        return new DefaultRuleModelParser();
    }

    @Bean
    public DefaultConditionEvaluator defaultConditionEvaluator() {
        return new DefaultConditionEvaluator();
    }

    @Bean
    public AntVG6RuleModelParserStrategy antVG6RuleModelParserStrategy() {
        return new AntVG6RuleModelParserStrategy();
    }


    @Bean(destroyMethod = "cleanup")
    public ClusterLocalScheduler clusterLocalScheduler(RuleEngineProperties properties,
                                                       Worker worker,
                                                       RpcServiceFactory rpcService) {
        ClusterLocalScheduler scheduler = new ClusterLocalScheduler(properties.getServerId(), rpcService);
        scheduler.addWorker(worker);
        return scheduler;
    }

    @Bean(initMethod = "setup", destroyMethod = "cleanup")
    public ClusterSchedulerRegistry schedulerRegistry(EventBus eventBus,
                                                      RpcServiceFactory rpcService,
                                                      Scheduler scheduler) {
        ClusterSchedulerRegistry registry = new ClusterSchedulerRegistry(eventBus, rpcService);
        registry.register(scheduler);
        registry.setKeepaliveInterval(Duration.ofSeconds(30));
        return registry;
    }

    @Bean
    public BeanPostProcessor autoRegisterStrategy(DefaultRuleModelParser defaultRuleModelParser,
                                                  DefaultConditionEvaluator defaultConditionEvaluator,
                                                  ClusterWorker worker) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof RuleModelParserStrategy) {
                    defaultRuleModelParser.register(((RuleModelParserStrategy) bean));
                }
                if (bean instanceof ConditionEvaluatorStrategy) {
                    defaultConditionEvaluator.register(((ConditionEvaluatorStrategy) bean));
                }
                if (bean instanceof TaskExecutorProvider) {
                    worker.addExecutor(((TaskExecutorProvider) bean));
                }

                return bean;
            }
        };
    }

    @Bean
    public ScriptEvaluator ruleEngineScriptEvaluator() {
        return new DefaultScriptEvaluator();
    }

    @Bean
    public ScriptConditionEvaluatorStrategy scriptConditionEvaluatorStrategy(ScriptEvaluator scriptEvaluator) {
        return new ScriptConditionEvaluatorStrategy(scriptEvaluator);
    }

    @Bean
    public ClusterWorker clusterWorker(RuleEngineProperties properties, EventBus eventBus, ClusterManager clusterManager, ConditionEvaluator evaluator) {
        return new ClusterWorker(properties.getServerId(), properties.getServerName(), eventBus, clusterManager, evaluator);
    }

    @Bean
    @ConditionalOnBean(TaskSnapshotRepository.class)
    public RuleEngine defaultRuleEngine(SchedulerRegistry registry,
                                        TaskSnapshotRepository repository,
                                        SchedulerSelector selector) {
        return new ClusterRuleEngine(registry, repository, selector);
    }

    @ConditionalOnClass(ElasticSearchService.class)
    static class ElasticSearchRuleEngineLogServiceConfiguration{

        @Bean
        @ConditionalOnBean(ElasticSearchService.class)
        public ElasticSearchRuleEngineLogService elasticSearchRuleEngineLogService(ElasticSearchService elasticSearchService,
                                                                                   ElasticSearchIndexManager indexManager){
            return new ElasticSearchRuleEngineLogService(indexManager,elasticSearchService);
        }

    }

//    @Bean
//    public DefaultRuleEngine defaultRuleEngine(Worker worker) {
//        LocalScheduler scheduler = new LocalScheduler("local");
//        scheduler.addWorker(worker);
//        return new DefaultRuleEngine(scheduler);
//    }
}
