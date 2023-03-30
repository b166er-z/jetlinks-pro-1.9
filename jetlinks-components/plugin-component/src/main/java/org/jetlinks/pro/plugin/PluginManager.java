package org.jetlinks.pro.plugin;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 插件管理,统一管理插件,安装,卸载.
 *
 * @since 1.2
 */
public interface PluginManager {

    /**
     * 根据ID获取插件,如果不存在则返回{@link Mono#empty()}
     *
     * @param id ID
     * @return 插件
     */
    Mono<Plugin> getPlugin(String id);

    /**
     * 获取全部可用的插件
     *
     * @return 插件信息
     */
    Flux<Plugin> getPlugins();

    /**
     * 根据ID获取插件提供商,如果不存在则返回{@link Mono#empty()}
     *
     * @param provider 插件提供商
     * @return 插件提供商
     */
    Mono<PluginProvider> getProvider(String provider);

    /**
     * 获取全部插件提供商
     *
     * @return 插件提供商
     */
    Flux<PluginProvider> getProviders();

    /**
     * 根据插件包配置安装插件,通常在首次安装插件时调用,会通知集群的全部节点都安装
     *
     * @param pluginPackage 插件包
     * @return 安装的插件列表
     */
    Flux<Plugin> install(PluginPackage pluginPackage);

    /**
     * 根据插件包加载插件信息,通常在服务启动时,加载已经安装过的插件信息到本地服务.
     *
     * @param pluginPackage 插件包
     * @return 加载的插件列表
     */
    Flux<Plugin> load(PluginPackage pluginPackage);

    /**
     * 根据ID卸载插件
     *
     * @param id 插件ID
     * @return 卸载结果
     * @see Plugin#getId()
     */
    Mono<Void> uninstall(String id);

}
