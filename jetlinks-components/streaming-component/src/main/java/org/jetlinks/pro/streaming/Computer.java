package org.jetlinks.pro.streaming;

import reactor.core.publisher.Mono;

public interface Computer<IN,OUT> {

    /**
     * 输入一个值到计算规则中,如果规则只是进行单次运算,那么会立即返回计算结果,否则需要通过{@link Streaming#output()}来获取输出结果.
     *
     * @param data 数据
     * @return 计算结果
     */
    Mono<OUT> compute(IN data);

}
