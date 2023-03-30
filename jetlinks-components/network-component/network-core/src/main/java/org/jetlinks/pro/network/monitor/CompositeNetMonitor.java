package org.jetlinks.pro.network.monitor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class CompositeNetMonitor implements NetMonitor {

    private final List<NetMonitor> monitors = new CopyOnWriteArrayList<>();

    public CompositeNetMonitor add(NetMonitor... monitors) {
        return add(Arrays.asList(monitors));
    }

    public CompositeNetMonitor add(Collection<NetMonitor> monitors) {
        this.monitors.addAll(monitors);
        return this;
    }

    protected void doWith(Consumer<NetMonitor> monitorConsumer) {
        monitors.forEach(monitorConsumer);
    }



    @Override
    public void buffered(int size) {
        doWith(netMonitor -> netMonitor.buffered(size));
    }

    @Override
    public void error(Throwable err) {
        doWith(netMonitor -> netMonitor.error(err));
    }

    @Override
    public void send() {
        doWith(NetMonitor::send);
    }

    @Override
    public void bytesSent(long bytesLength) {
        doWith(netMonitor -> netMonitor.bytesSent(bytesLength));
    }

    @Override
    public void bytesRead(long bytesLength) {
        doWith(netMonitor -> netMonitor.bytesRead(bytesLength));
    }

    @Override
    public void sendComplete() {
        doWith(NetMonitor::sendComplete);
    }

    @Override
    public void sendError(Throwable err) {
        doWith(monitor->monitor.sendError(err));
    }

    @Override
    public void handled() {
        doWith(NetMonitor::handled);
    }

    @Override
    public void connected() {
        doWith(NetMonitor::connected);
    }

    @Override
    public void disconnected() {
        doWith(NetMonitor::disconnected);
    }
}
