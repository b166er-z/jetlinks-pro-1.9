package org.jetlinks.pro.notify;

/**
 * 通知类型.通常使用枚举实现此接口
 *
 * @author zhouhao
 * @see DefaultNotifyType
 * @since 1.0
 */
public interface NotifyType {

    String getId();

    String getName();
}
