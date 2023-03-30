package org.jetlinks.pro.rule.engine.editor;

import org.springframework.core.io.Resource;

import java.util.Map;
import java.util.Set;

/**
 * 规则引擎编辑器节点
 *
 * @author zhouhao
 * @since 1.9.0
 */
public interface EditorNode extends Comparable<EditorNode> {

    /**
     * @return ID
     */
    String getId();

    /**
     * @return 节点名称
     */
    String getName();

    Set<String> getTypes();

    /**
     * 获取节点资源信息
     *
     * @return 节点资源信息
     */
    Map<ResourceType, Resource> getEditorResources();

    /**
     * @return 是否已启用
     */
    boolean isEnabled();

    /**
     * @return 排序序号
     */
    int getOrder();

    @Override
    default int compareTo(EditorNode o) {
        return Integer.compare(getOrder(), o.getOrder());
    }
}
