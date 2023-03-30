package org.jetlinks.pro.rule.engine.model.nodes;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;

/**
 * 节点转换器，用于将NodeRed的配置转换为平台的规则节点信息
 *
 * @author zhouhao
 * @since 1.3
 */
public interface NodeConverter {

    /**
     * @return Node-Red中的节点类型
     */
    String getNodeType();

    /**
     * 转换节点模型
     *
     * @param nodeJson 节点配置json
     * @return 节点模型
     */
    RuleNodeModel convert(JSONObject nodeJson);

    /**
     * @return 此节点类型是否为事件监听器
     */
    default boolean isEventListener() {
        return false;
    }

    /**
     * 如果是事件监听器，则返回事件监听器类型
     *
     * @return 事件监听器类型
     */
    default String getEventType() {
        return null;
    }

}
