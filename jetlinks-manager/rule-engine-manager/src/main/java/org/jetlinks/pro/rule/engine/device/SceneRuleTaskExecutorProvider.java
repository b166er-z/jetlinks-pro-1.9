package org.jetlinks.pro.rule.engine.device;

import lombok.AllArgsConstructor;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class SceneRuleTaskExecutorProvider implements TaskExecutorProvider {

    private final EventBus eventBus;

    @Override
    public String getExecutor() {
        return "scene";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {

        return Mono.just(new DeviceSceneTaskExecutor(context));
    }

    class DeviceSceneTaskExecutor extends FunctionTaskExecutor {

        private String id;

        private String name;

        private SceneRule.InspireMode inspireMode;//触发器激发模式
        private long timestamp;//触发时刻
        private int num;//触发器触动的个数
        private int total;//触发器总个数

        public DeviceSceneTaskExecutor(ExecutionContext context) {
            super("场景联动", context);
            reload();
        }

        @Override
        public void reload() {
            this.num=0;
            this.timestamp=0L;
            this.total = getContext().getJob().getInputs().size();
            this.id = (String) getContext().getJob().getConfiguration().get("id");
            this.name = (String) getContext().getJob().getConfiguration().get("name");
            this.inspireMode = SceneRule.InspireMode.valueOf((String)getContext().getJob().getConfiguration().get("inspireMode"));
        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            Map<String, Object> data = new HashMap<>();
            data.put("sceneId", id);
            data.put("sceneName", name);
            data.put("executeTime", System.currentTimeMillis());

            input.acceptMap(data::putAll);

            //以下是场景时间段判断
            String cron = getContext().getJob().getConfiguration().get("cron")+"";
            if(!StringUtils.isEmpty(cron)){
                String[] time = cron.split("#");
                LocalDate d = LocalDate.now();
                LocalTime t = LocalTime.now();
                String w = d.getDayOfWeek().getValue()%7 + "";
                if(!time[0].contains(w) || t.isBefore(LocalTime.parse(time[1])) || t.isAfter(LocalTime.parse(time[2]))){
                    return Mono.empty();
                }
            }

            //以下是触发器激发模式判断
            if(inspireMode==SceneRule.InspireMode.all){
                if(timestamp==0L){
                    timestamp = Long.parseLong(data.get("timestamp") + "");
                    ++num;
                } else {
                    long temp = Long.parseLong(data.get("timestamp") + "");
                    if(Math.abs(temp - timestamp)<3){
                        timestamp = temp;
                        ++num;
                    }
                }
                if(num<total)return Mono.empty();
            }
            num=0;
            timestamp=0L;

            return eventBus
                .publish(String.join("/", "scene", id), data)
                .thenReturn(context.newRuleData(input.newData(data)));
        }
    }
}
