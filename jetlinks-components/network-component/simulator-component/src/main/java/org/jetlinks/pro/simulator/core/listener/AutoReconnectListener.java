package org.jetlinks.pro.simulator.core.listener;

import lombok.AllArgsConstructor;
import org.jetlinks.pro.simulator.core.Session;
import org.jetlinks.pro.simulator.core.Simulator;
import org.jetlinks.pro.simulator.core.SimulatorListener;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自动重连监听器，用于会话断开连接后自动重连
 *
 * @author zhouhao
 * @since 1.6
 */
@AllArgsConstructor
public class AutoReconnectListener implements SimulatorListener {

    private final String id;

    //延迟间隔
    private final Duration[] delays;

    //最大重试次数
    private final int maxTimes;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "auto-reconnect";
    }

    @Override
    public boolean supported(Simulator simulator) {
        return true;
    }

    @Override
    public void init(Simulator simulator) {

    }

    @Override
    public void before(Session session) {

    }

    @Override
    public void after(Session session) {
        AtomicInteger times = new AtomicInteger();
        Runnable doReconnect = () -> {
            if (session.isConnected()) {
                return;
            }
            int currentTimes = Math.min(times.get(), delays.length - 1);
            if (maxTimes > 0 && times.incrementAndGet() >= maxTimes) {
                return;
            }
            Duration delay = this.delays[currentTimes];
            Mono.delay(delay)
                .flatMap(ignore -> session.connect())
                .subscribe();
        };
        //监听错误和断开连接事件
        session.onError(err -> doReconnect.run());
        session.onDisconnected(doReconnect);
        //连接成功后重置次数
        session.onConnected(() -> times.set(0));
    }

    @Override
    public void shutdown() {

    }
}
