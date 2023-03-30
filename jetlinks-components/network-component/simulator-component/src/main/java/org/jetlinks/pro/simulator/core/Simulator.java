package org.jetlinks.pro.simulator.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 网络模拟器
 *
 * @author zhouhao
 * @since 1.6
 */
public interface Simulator {

    /**
     * 获取监听器
     *
     * @param id 监听器ID
     * @return 监听器
     */
    Mono<SimulatorListener> getListener(String id);

    /**
     * 注册监听器
     *
     * @param listener 监听器
     */
    void registerListener(SimulatorListener listener);

    /**
     * 获取模拟器类型，如：MQTT，TCP
     *
     * @return 模拟器类型
     */
    String getType();

    /**
     * @return 模拟器当前状态信息
     */
    Mono<State> state();

    /**
     * 启动模拟器
     *
     * @return void
     */
    Mono<Void> start();

    /**
     * 停止模拟器
     *
     * @return void
     */
    Mono<Void> shutdown();

    /**
     * 根据ID获取会话，如果会话不存在则返回{@link Mono#empty()}
     *
     * @param id ID
     * @return 会话
     */
    Mono<Session> getSession(String id);

    /**
     * @return 会话数量
     */
    int totalSession();

    /**
     * @return 是否运行中
     */
    boolean isRunning();

    /**
     * @return 全部会话
     */
    Flux<Session> getSessions();

    /**
     * 获取指定数量的会话
     *
     * @param total 数量
     * @return 会话流
     */
    default Flux<Session> getSessions(int total) {
        int current = totalSession();
        if (total >= current) {
            return getSessions();
        }

        int offset = ThreadLocalRandom.current().nextInt(0, current - total);

        return getSessions(offset, total);
    }

    /**
     * 指定偏移量和总数获取会话（分页）
     *
     * @param offset 偏移量
     * @param total  总数
     * @return 会话流
     */
    default Flux<Session> getSessions(int offset, int total) {
        return getSessions()
            .skip(offset)
            .take(total);
    }

    /**
     * 输出日志，支持表达式如:
     * <pre>
     *     simulator.log("发送报文:{}",payload);
     * </pre>
     *
     * @param text 日志内容
     * @param args 参数
     */
    void log(String text, Object... args);

    /**
     * 监听日志
     *
     * @return 日志流
     */
    Flux<String> handleLog();

    /**
     * 监听启动完成事件，当模拟器启动完成后执行事件监听器
     * @param runnable 事件监听器
     */
    void doOnComplete(Runnable runnable);

    /**
     * 监听模拟器关闭事件，当模拟器停止时执行监听器
     * @param disposable 监听器
     */
    void doOnClose(Disposable disposable);

    /**
     * 延迟执行任务，调用返回值{@link Disposable#dispose()}可终止任务
     * @param task 任务
     * @param delay 延迟时间，单位：毫秒
     * @return Disposable
     */
    default Disposable delay(Runnable task, long delay) {
        return Schedulers.parallel().schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 定时执行任务，调用返回值{@link Disposable#dispose()}可终止任务
     * @param task 任务
     * @param period 执行间隔，单位：毫秒
     * @return Disposable
     */
    default Disposable timer(Runnable task, long period) {
        return Schedulers.parallel().schedulePeriodically(task, period, period, TimeUnit.MILLISECONDS);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    class DistTime {
        @Schema(description = "时间区间")
        private String range;

        @Schema(description = "数量")
        private long number;

        public static DistTime of(List<Map.Entry<Integer, Long>> entry) {
            if (entry.size() == 1) {
                Map.Entry<Integer, Long> e = entry.get(0);
                return new DistTime(">=" + e.getKey() + "ms", e.getValue());
            }
            Map.Entry<Integer, Long> e = entry.get(0);
            return new DistTime(e.getKey() + "ms~" + entry.get(1).getKey() + "ms", e.getValue());
        }
    }

    @Getter
    @Setter
    class State {
        //已完成
        @Schema(description = "是否已完成")
        private boolean complete;
        //总数
        @Schema(description = "总连接数")
        private long total;
        //当前数量
        @Schema(description = "并发数")
        private long current;
        //失败数量
        @Schema(description = "失败数量")
        private long failed;
        //连接时间统计
        @Schema(description = "连接时间统计")
        private Agg aggTime;

        //时间分布
        @Schema(description = "时间分布,key为时间,value为数量")
        private Map<Integer, Long> distTime;

        //失败类型计数
        @Schema(description = "失败计数,key为失败类型,value为数量")
        private Map<String, Long> failedTypeCounts;

        @Schema(description = "运行时统计")
        private Runtime runtime;

        @Schema(description = "运行统计历史")
        private List<Runtime> runtimeHistory;

        @Schema(description = "时间分布集合")
        public List<DistTime> getDistTimeList() {
            if (CollectionUtils.isEmpty(distTime)) {
                return Collections.emptyList();
            }
            return Flux
                .fromIterable(distTime.entrySet())
                .window(2, 1)
                .flatMap(window -> window
                    .collectList()
                    .map(DistTime::of))
                .collectList()
                .toFuture()
                .getNow(Collections.emptyList());
        }


        @Getter
        @Setter
        public static class Runtime {
            @Schema(description = "时间")
            String time;

            //上行数量
            @Schema(description = "总计上行数量")
            long totalUpstream;
            //上行流量
            @Schema(description = "总计上行流量")
            long totalUpstreamBytes;

            //下行数量
            @Schema(description = "总计下行数量")
            long totalDownstream;

            //下行流量
            @Schema(description = "总计下行流量")
            long totalDownstreamBytes;
        }

        @Getter
        @Setter
        public static class Agg {
            @Schema(description = "总耗时")
            int total;
            @Schema(description = "最大耗时")
            int max;
            @Schema(description = "最小耗时")
            int min;
            @Schema(description = "平均耗时")
            int avg;
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this, SerializerFeature.PrettyFormat);
        }
    }

}
