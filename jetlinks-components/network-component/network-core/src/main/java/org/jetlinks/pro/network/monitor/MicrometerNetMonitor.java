package org.jetlinks.pro.network.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * @author wangzheng
 * @since 1.0
 */
public class MicrometerNetMonitor implements NetMonitor {

    private final MeterRegistry meterRegistry;

    private final String id;

    private final String[] tags;


    public MicrometerNetMonitor(MeterRegistry meterRegistry, String id, String[] tags) {
        this.meterRegistry = meterRegistry;
        this.id = id;
        this.tags = tags;

        bytesReadCounter = Counter
            .builder(id)
            .tags(tags)
            .tag("target", "bytesRead")
            .register(meterRegistry);

        bytesSentCounter = Counter
            .builder(id)
            .tags(tags)
            .tag("target", "bytesSent")
            .register(meterRegistry);

        sendCounter = Counter
            .builder(id)
            .tags(tags)
            .tag("target", "send")
            .register(meterRegistry);

        readCounter = Counter
            .builder(id)
            .tags(tags)
            .tag("target", "read")
            .register(meterRegistry);

        connectCounter = Counter.builder(id)
            .tags(tags)
            .tag("target", "connected")
            .register(meterRegistry);
        disconnectCounter = Counter.builder(id)
            .tags(tags)
            .tag("target", "disconnected")
            .register(meterRegistry);

    }

    final Counter bytesReadCounter;
    final Counter bytesSentCounter;
    final Counter sendCounter;
    final Counter readCounter;
    final Counter connectCounter;
    final Counter disconnectCounter;

    @Override
    public void buffered(int size) {
        Gauge
            .builder(id, size, Number::doubleValue)
            .tags(tags)
            .tag("target", "buffered")
            .register(meterRegistry);
    }

    @Override
    public void bytesRead(long bytesLength) {
        readCounter.increment();
        bytesReadCounter.increment(bytesLength);
    }

    @Override
    public void bytesSent(long bytesLength) {
        sendCounter.increment();
        bytesSentCounter.increment(bytesLength);
    }

    @Override
    public void error(Throwable err) {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "error")
            .tag("message", err == null ? "null" : String.valueOf(err.getMessage()))
            .tag("errorType", err == null ? "null" : err.getClass().getSimpleName())
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void send() {

    }

    @Override
    public void sendComplete() {

    }

    @Override
    public void sendError(Throwable err) {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "sendError")
            .tag("message", err == null ? "null" : String.valueOf(err.getMessage()))
            .tag("errorType", err == null ? "null" : err.getClass().getSimpleName())
            .register(meterRegistry)
            .increment();
    }

    @Override
    public Timer timer() {
        // TODO: 2019/12/26
        return new Timer() {
            @Override
            public void start() {

            }

            @Override
            public void end() {

            }
        };
    }

    @Override
    public void handled() {

    }

    @Override
    public void connected() {
        connectCounter.increment();
    }

    @Override
    public void disconnected() {
        disconnectCounter.increment();
    }

}
