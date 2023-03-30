package org.jetlinks.pro.rule.engine.web.editor;

import io.swagger.v3.oas.annotations.Hidden;
import org.jetlinks.pro.rule.engine.executor.TimerTaskExecutorProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.Date;

@RestController
@RequestMapping("/rule-engine/utils")
@Hidden
public class RuleEngineUtilsController {

    @GetMapping("/cron/times")
    public Flux<String> getCronLastExecuteTime(@RequestParam String cron,
                                               @RequestParam(required = false, defaultValue = "10") long times) {
        return TimerTaskExecutorProvider
            .getLastExecuteTimes(cron,new Date(),times)
            .map(time->time.format(DateTimeFormatter.ISO_DATE_TIME))
            .onErrorResume(err-> Mono.just("请输入正确的cron表达式"));
    }
}
