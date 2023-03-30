package org.jetlinks.pro.simulator.core;

import reactor.core.publisher.Mono;

/**
 * 模拟器管理器，用于创建和管理模拟器
 *
 * @author zhouhao
 * @since 1.6
 */
public interface SimulatorManager {

    /**
     * 根据ID获取模拟器
     *
     * @param id ID
     * @return 模拟器
     */
    Mono<Simulator> getSimulator(String id);

    /**
     * 移除模拟器
     *
     * @param id ID
     * @return void
     */
    Mono<Void> remove(String id);

    /**
     * 根据模拟器配置创建模拟器
     *
     * @param config 模拟器配置
     * @return 模拟器
     */
    Mono<Simulator> createSimulator(SimulatorConfig config);
}
