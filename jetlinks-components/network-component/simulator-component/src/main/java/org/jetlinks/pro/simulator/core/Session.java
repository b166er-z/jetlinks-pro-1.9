package org.jetlinks.pro.simulator.core;

import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 模拟器会话
 *
 * @author zhouhao
 * @see org.jetlinks.pro.simulator.mqtt.MqttSession
 * @see org.jetlinks.pro.simulator.tcp.TcpSession
 * @since 1.6
 */
public interface Session {

    /**
     * @return 会话ID
     */
    String getId();

    /**
     * @return 序号
     */
    int getIndex();

    /**
     * @return 会话创建时间
     */
    long getCreateTime();

    /**
     * @return 是否已经连接
     */
    boolean isConnected();

    /**
     * @return 连接时间
     */
    long getConnectTime();

    /**
     * @return 连接耗时
     */
    default long getConnectUseTime() {
        return getConnectTime() - getCreateTime();
    }

    /**
     * 监听连接事件，调用返回值{@link Disposable#dispose()}取消监听
     *
     * @param listener 事件监听器
     * @return Disposable
     */
    Disposable onConnected(Runnable listener);

    /**
     * 监听连接前事件，调用返回值{@link Disposable#dispose()}取消监听
     *
     * @param listener 事件监听器
     * @return Disposable
     */
    Disposable onConnectBefore(Runnable listener);

    /**
     * 监听断开连接事件，调用返回值{@link Disposable#dispose()}取消监听
     *
     * @param listener 监听器
     * @return Disposable
     */
    Disposable onDisconnected(Runnable listener);

    /**
     * 监听错误事件，调用返回值{@link Disposable#dispose()}取消监听
     *
     * @param listener 监听器
     * @return Disposable
     */
    Disposable onError(Consumer<Throwable> listener);

    /**
     * 监听上行消息，调用返回值{@link Disposable#dispose()}取消监听
     *
     * @param listener 监听器
     * @return Disposable
     */
    Disposable onUpstream(Consumer<EncodedMessage> listener);

    /**
     * 监听下行消息，调用返回值{@link Disposable#dispose()}取消监听
     *
     * @param listener 监听器
     * @return Disposable
     */
    Disposable onDownstream(Consumer<EncodedMessage> listener);

    /**
     * 异步推送格式化的消息，不同类型的会话支持的格式不同.
     * <p>
     * MQTT:
     * <pre>
     * QoS1 /topic
     *
     * {"key":"value"}
     * </pre>
     * TCP:
     * <pre>
     *   0x1F //以16进制发送数据
     *   abc //直接发送字符串
     * </pre>
     *
     * @param formatBody 格式化消息
     * @return Mono
     * @see org.jetlinks.core.message.codec.SimpleMqttMessage#of(String)
     */
    Mono<Void> publishAsync(String formatBody);

    /**
     * 关闭会话
     */
    void close();

    /**
     * @return 获取会话最近的错误信息
     */
    Optional<Throwable> lastError();

    /**
     * 执行连接
     *
     * @return Mono
     */
    Mono<Void> connect();
}
