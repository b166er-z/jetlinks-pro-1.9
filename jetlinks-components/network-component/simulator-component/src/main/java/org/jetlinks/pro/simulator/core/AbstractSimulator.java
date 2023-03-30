package org.jetlinks.pro.simulator.core;

import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.utils.ExpressionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 抽象模拟器实现，实现模拟器通用功能
 *
 * @author zhouhao
 * @since 1.6
 */
public abstract class AbstractSimulator implements Simulator {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    protected final List<SimulatorListener> listeners = new CopyOnWriteArrayList<>();

    protected final Map<String, Session> sessions = new ConcurrentHashMap<>();

    protected final SimulatorConfig config;

    protected final AddressPool addressPool;

    protected final SimulatorListenerBuilder listenerBuilder;

    private final EmitterProcessor<String> logProcessor = EmitterProcessor.create(false);
    private final FluxSink<String> logSink = logProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final AtomicLong connection = new AtomicLong();
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong success = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong maxTime = new AtomicLong();
    private final static int initMinTime = Integer.MAX_VALUE;

    private final AtomicLong minTime = new AtomicLong(initMinTime);
    private final AtomicLong avgTime = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean complete = new AtomicBoolean();

    //上下行统计
    private final AtomicLong totalUpstream = new AtomicLong();
    private final AtomicLong totalUpstreamBytes = new AtomicLong();
    private final AtomicLong totalDownstream = new AtomicLong();
    private final AtomicLong totalDownstreamBytes = new AtomicLong();


    private final Map<String, AtomicLong> errorCounter = new ConcurrentHashMap<>();

    private final Map<Integer, AtomicLong> dist = new ConcurrentHashMap<>();

    private final List<Runnable> completeListener = new CopyOnWriteArrayList<>();

    private final Disposable.Composite disposable = Disposables.composite();

    //时间分布
    int[] distArray = {5000, 1000, 500, 100, 20, 0};

    private final Deque<State.Runtime> history = new LinkedList<>();

    public AbstractSimulator(SimulatorConfig config, SimulatorListenerBuilder builder, AddressPool pool) {
        this.addressPool = pool;
        this.config = config;
        for (int i : distArray) {
            dist.put(i, new AtomicLong());
        }
        this.listenerBuilder = builder;
    }

    protected String processExpression(String expression, Map<String, Object> context) {
        return ExpressionUtils.analytical(expression, context, "spel");
    }

    protected abstract Mono<? extends Session> createSession(int index, String bind);

    @Override
    public Mono<SimulatorListener> getListener(String id) {
        return Flux.fromIterable(listeners)
                   .filter(s -> s.getId().equals(id))
                   .singleOrEmpty();
    }

    @Override
    public void registerListener(SimulatorListener listener) {
        if (!listener.supported(this)) {
            return;
        }
        listener.init(this);
        listeners.add(listener);
        listeners.sort(Comparator.comparing(SimulatorListener::order));
    }


    protected void fireListener(Consumer<SimulatorListener> listenerConsumer) {
        listeners.forEach(listenerConsumer);
    }

    protected void before(Session session) {
        connection.incrementAndGet();
        session.onError(this::error);
        fireListener(listener -> listener.before(session));
    }

    protected void connectSuccess(Session session) {
        fireListener(listener -> listener.after(session));
        long time = session.getConnectUseTime();
        totalTime.addAndGet(time);

        maxTime.updateAndGet(v -> Math.max(time, v));
        minTime.updateAndGet(v -> Math.min(time, v));
        avgTime.set(totalTime.get() / total.get());
        success.incrementAndGet();

        session.onDisconnected(success::decrementAndGet);

        session.onUpstream(msg -> {
            long index = msg.getPayload().writerIndex();
            totalUpstreamBytes.addAndGet(index);
            totalUpstream.incrementAndGet();
        });

        session.onDownstream(msg -> {
            long index = msg.getPayload().writerIndex();
            totalDownstreamBytes.addAndGet(index);
            totalDownstream.incrementAndGet();
        });

        for (int i : distArray) {
            if (time >= i) {
                dist.computeIfAbsent(i, ignore -> new AtomicLong(i)).incrementAndGet();
                break;
            }
        }
    }

    protected void after(Session session) {
        total.incrementAndGet();
        connection.decrementAndGet();
        sessions.put(session.getId(), session);
        if (session.isConnected()) {
            connectSuccess(session);
        } else {
            session.onConnected(() -> {
                connectSuccess(session);
            });
            session
                .lastError()
                .ifPresent(this::error);
        }

    }

    private void error(Throwable error) {
        log(error.getMessage(), error);
        failed.incrementAndGet();
        errorCounter
            .computeIfAbsent(convertErrorMessage(error), i -> new AtomicLong())
            .incrementAndGet();

    }

    protected String convertErrorMessage(Throwable error) {
        return (error.getMessage() == null
            ? error.getClass().getSimpleName()
            : error.getMessage()) + ThreadLocalRandom.current().nextInt(0, 5);
    }

    private Mono<Void> createConnectTask(int index, int total) {
        return Flux
            .range(index, total)
            .flatMap(i -> {

                String bind = addressPool
                    .take(config.getRunner().getBinds())
                    .orElseThrow(() -> new UnsupportedOperationException("网络地址资源不够!"));

                return this
                    .createSession(i, bind)
                    .doOnNext(session -> session.onDisconnected(() -> addressPool.release(bind)));

            })
            .doOnNext(this::before)
            .flatMap(session ->
                         Mono.<Session>create(sink ->
                                                  session
                                                      .connect()
                                                      .doFinally(ignore -> sink.success(session))
                                                      .subscribe())
            )
            .doOnNext(this::after)
            .then()
            .doOnSuccess(ignore -> this.log("create simulators[{}] complete!", total))
            .onErrorContinue((err, v) -> this.error(err));
    }

    @Override
    public Mono<State> state() {
        return Mono.fromCallable(() -> {
            State state = new State();
            state.setComplete(complete.get());
            state.setCurrent(connection.get());
            state.setFailed(failed.get());
            state.setTotal(total.get());
            state.setDistTime(dist
                                  .entrySet()
                                  .stream()
                                  .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));

            state.setFailedTypeCounts(errorCounter
                                          .entrySet()
                                          .stream()
                                          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));

            State.Agg agg = new State.Agg();
            agg.avg = avgTime.intValue();
            agg.max = maxTime.intValue();
            agg.min = minTime.intValue();
            if (agg.min == initMinTime) {
                agg.min = 0;
            }
            agg.total = totalTime.intValue();
            state.setAggTime(agg);


            state.setRuntime(createRuntime());
            state.setRuntimeHistory(getRuntimeHistory());
            return state;
        });
    }

    private List<State.Runtime> getRuntimeHistory() {
        return new ArrayList<>(history);
    }

    private State.Runtime createRuntime() {
        State.Runtime runtime = new State.Runtime();
        runtime.totalDownstream = totalDownstream.get();
        runtime.totalDownstreamBytes = totalDownstreamBytes.get();
        runtime.totalUpstream = totalUpstream.get();
        runtime.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("mm:ss"));
        runtime.totalUpstreamBytes = totalUpstreamBytes.get();
        return runtime;
    }

    private void resetRuntime() {
        totalDownstream.set(0);
        totalDownstreamBytes.set(0);
        totalUpstream.set(0);
        totalUpstreamBytes.set(0);
    }

    public void doStart() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        if (!CollectionUtils.isEmpty(config.getListeners())) {
            for (SimulatorConfig.Listener listener : config.getListeners()) {
                registerListener(listenerBuilder.build(listener));
            }
        }
        int start = config.getRunner().getStartWith();
        int total = config.getRunner().getTotal();
        int batch = config.getRunner().getBatch();
        List<Mono<Void>> jobs = new ArrayList<>();

        //创建连接任务
        int batchSize = total / batch;
        for (int i = 0; i < batchSize; i++) {
            total -= batch;
            jobs.add(createConnectTask(start + i * batch, batch));
        }
        if (total > 0) {
            jobs.add(createConnectTask(start + batchSize * batch, total));
        }
        //每5秒统计监控信息
        disposable
            .add(Flux
                     .interval(Duration.ofSeconds(5))
                     .subscribe(v -> {
                         history.addLast(createRuntime());
                         resetRuntime();
                         if (history.size() > 30) {
                             history.removeFirst();
                         }
                     }));
        //开始执行连接任务
        disposable.add(Flux.concat(jobs)
                           .doFinally(f -> complete())
                           .doOnError(this::error)
                           .subscribe());
    }

    private void complete() {
        complete.set(true);
        completeListener.forEach(Runnable::run);
    }

    public void log(String msg, Object... args) {
        log.debug(msg, args);
        if (logProcessor.hasDownstreams()) {
            String log = MessageFormatter.arrayFormat(msg, args).getMessage();
            logSink.next(log);
        }
    }

    @Override
    public Flux<String> handleLog() {
        return logProcessor;
    }

    @Override
    public void doOnComplete(Runnable runnable) {
        completeListener.add(runnable);
    }

    @Override
    public void doOnClose(Disposable disposable) {
        this.disposable.add(disposable);
    }

    @Override
    public Disposable delay(Runnable task, long delay) {
        Disposable disposable = Schedulers.parallel().schedule(task, delay, TimeUnit.MILLISECONDS);
        this.disposable.add(disposable);
        return disposable;
    }

    @Override
    public Disposable timer(Runnable task, long period) {
        Disposable disposable = Schedulers.parallel().schedulePeriodically(task, period, period, TimeUnit.MILLISECONDS);
        this.disposable.add(disposable);
        return disposable;
    }

    @Override
    public Mono<Void> start() {

        return Mono.fromRunnable(this::doStart);
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono
            .fromRunnable(() -> {
                for (Session value : sessions.values()) {
                    value.close();
                }
                sessions.clear();
                fireListener(SimulatorListener::shutdown);
                disposable.dispose();
                started.set(false);
            });
    }

    @Override
    public int totalSession() {
        return sessions.size();
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    @Override
    public Mono<Session> getSession(String id) {
        return Mono.justOrEmpty(sessions.get(id));
    }

    @Override
    public Flux<Session> getSessions() {
        return Flux.fromIterable(sessions.values());
    }

}
