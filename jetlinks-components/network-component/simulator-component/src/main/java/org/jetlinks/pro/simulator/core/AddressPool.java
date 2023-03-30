package org.jetlinks.pro.simulator.core;

import java.util.List;
import java.util.Optional;

/**
 * 地址池，用于管理可分配端口的IP地址（网卡），因为一个ip可分配的端口只有65535，
 * 通常来说，要模拟超过6万+连接时，则需要使用多网卡，或者虚拟网卡来突破此限制
 *
 * @author zhouhao
 * @since 1.6
 */
public interface AddressPool {

    /**
     * @return 全部可以地址
     */
    List<String> getAllAddress();

    /**
     * 从指定的地址列表中选择可用的地址，使用完后应该释放{@link this#release(String)}
     *
     * @param addresses 地址列表
     * @return 可用的地址
     */
    Optional<String> take(List<String> addresses);

    /**
     * 释放地址
     *
     * @param address 地址
     */
    void release(String address);

}
