package org.jetlinks.pro.network.monitor;

/**
 * 网络组件监控器,用于进行网络组件监控，统计网络流量等信息
 *
 * @author zhouhao
 * @since 1.0
 */
public interface NetMonitor {

    /**
     * 当前已缓冲等数量
     *
     * @param size 数量
     */
    @Deprecated
    void buffered(int size);

    /**
     * 处理错误
     *
     * @param err 错误信息
     */
    void error(Throwable err);

    /**
     * 发送请求
     *
     * @deprecated 已弃用，意义不大
     */
    @Deprecated
    void send();

    /**
     * 上报发送字节数
     *
     * @param bytesLength 发送字节数
     */
    default void bytesSent(long bytesLength) {

    }

    /**
     * 上报读取字节数
     *
     * @param bytesLength 读取字节数
     */
    default void bytesRead(long bytesLength) {

    }

    /**
     * 获取计时器,用于统计某个操作的耗时
     *
     * @return Timer
     */
    default Timer timer() {
        return Timer.none;
    }

    /**
     * 统计发送完成
     *
     * @deprecated 已弃用，意义不大
     */
    @Deprecated
    void sendComplete();

    /**
     * 发送数据错误
     *
     * @param err 错误信息
     */
    void sendError(Throwable err);

    /**
     * @deprecated 意义不大
     */
    @Deprecated
    void handled();

    /**
     * 统计已连接数量+1
     */
    void connected();

    /**
     * 统计断开连接数量+1
     */
    void disconnected();

    interface Timer {
        /**
         * 操作开始
         */
        void start();

        /**
         * 操作结束
         */
        void end();

        Timer none = new Timer() {
            @Override
            public void start() {

            }

            @Override
            public void end() {

            }
        };
    }
}
