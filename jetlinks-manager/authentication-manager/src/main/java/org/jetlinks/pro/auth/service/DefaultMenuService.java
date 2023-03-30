package org.jetlinks.pro.auth.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.crud.service.ReactiveTreeSortEntityService;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.pro.auth.entity.MenuEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单基础信息管理
 * <p>
 *  使用通用增删改查接口实现，同时实现通用树排序接口保证菜单数据的树状结构.
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Service
public class DefaultMenuService
        extends GenericReactiveCrudService<MenuEntity, String>
        implements ReactiveTreeSortEntityService<MenuEntity, String> {
    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.MD5;
    }

    @Override
    public void setChildren(MenuEntity menuEntity, List<MenuEntity> children) {
        menuEntity.setChildren(children);
    }

    @Override
    public List<MenuEntity> getChildren(MenuEntity menuEntity) {
        return menuEntity.getChildren();
    }
}
