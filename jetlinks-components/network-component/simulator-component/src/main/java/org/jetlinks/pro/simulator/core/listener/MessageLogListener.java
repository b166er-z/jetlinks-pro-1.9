package org.jetlinks.pro.simulator.core.listener;

import lombok.Getter;
import org.jetlinks.pro.simulator.core.Session;
import org.jetlinks.pro.simulator.core.Simulator;
import org.jetlinks.pro.simulator.core.SimulatorListener;

import java.util.Map;

/**
 * 消息日志监听器，用于打印模拟器运行过程相关日志
 *
 * @author zhouhao
 * @since 1.6
 */
public class MessageLogListener implements SimulatorListener {
    @Getter
    private final String id;

    private Simulator simulator;

    public MessageLogListener(String id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return "message";
    }

    @Override
    public boolean supported(Simulator simulator) {
        return true;
    }

    @Override
    public void init(Simulator simulator) {
        this.simulator = simulator;
        this.simulator
            .doOnComplete(() -> simulator
                .state()
                .subscribe(state -> {
                    //启动状态信息
                    StringBuilder builder = new StringBuilder();
                    long total = state.getTotal();
                    builder.append("=======complete(total:").append(total).append(")==========").append("\n");
                    //最大耗时
                    builder.append("max: ").append(state.getAggTime().getMax()).append("ms").append("\n");
                    //最小耗时
                    builder.append("min: ").append(state.getAggTime().getMin()).append("ms").append("\n");
                    //平均耗时
                    builder.append("avg: ").append(state.getAggTime().getAvg()).append("ms").append("\n");
                    //时间分布
                    for (Map.Entry<Integer, Long> entry : state.getDistTime().entrySet()) {
                        builder.append("> ").append(entry.getKey()).append("ms")
                               .append(": ").append(entry.getValue())
                               .append("(")
                               .append(total == 0 ? 0 : String.format("%.1f", (entry.getValue() / (double) total) * 100))
                               .append("%)")
                               .append("\n");
                    }
                    simulator.log("\n{}", builder);
                }));
    }

    @Override
    public void before(Session session) {
    }

    @Override
    public void after(Session session) {
        //打印消息上行日志
        session.onDownstream(msg -> simulator.log("{} downstream => \n{}", session, msg));
        //打印消息下行日志
        session.onUpstream(msg -> simulator.log("{} upstream <= \n{}", session, msg));
        //打印断开连接日志
        session.onDisconnected(() -> simulator.log("{} disconnect", session));
    }

    @Override
    public void shutdown() {

    }

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }
}
