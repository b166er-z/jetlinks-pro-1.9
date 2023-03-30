package org.jetlinks.pro.streaming;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 流式计算基础接口,此接口表示运行中的一个计算规则,通过{@link Streaming#compute(Object)}输入值,
 * 通过{@link Streaming#output()}获取流式输出结果
 *
 * @author zhouhao
 * @see 1.9
 */
public interface Streaming<IN, OUT, STREAM_OUT> extends Computer<IN, OUT>, Disposable {

    /**
     * 获取计算输出结果流
     *
     * @return 计算结果
     */
    Flux<STREAM_OUT> output();

}
