package com.zxch.hotswap.zk;


import com.zxch.hotswap.IHotSwapCallback;
import com.zxch.hotswap.IHotSwapProvider;
import com.zxch.hotswap.util.HotSwapUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ZooKeeper 热加载监听器
 * <p>
 * 监听 ZK 节点变化，自动触发热加载
 * 支持一个节点变更执行多个 Provider
 * 
 * @author zhangxun
 */
public class ZkHotSwapListener implements TreeCacheListener {

    private static final Logger logger = LoggerFactory.getLogger(ZkHotSwapListener.class);

    /**
     * 是否启用 ZK 字节码功能
     * true: 直接使用 ZK 节点的 data 作为字节码
     * false: 使用自定义 Provider 获取字节码
     */
    private final boolean useZkBytes;

    /**
     * 自定义的字节码提供者列表（当 useZkBytes=false 时使用）
     * 支持多个 Provider，node 变更时会依次执行所有 Provider
     */
    private final List<IHotSwapProvider> customProviders;

    /**
     * 热加载回调
     */
    private final IHotSwapCallback callback;

    /**
     * 构造函数 - 使用 ZK 节点数据作为字节码
     * 
     * @param callback 热加载回调（可为null）
     */
    public ZkHotSwapListener(IHotSwapCallback callback) {
        this(true, Collections.emptyList(), callback);
    }

    /**
     * 构造函数 - 使用单个自定义 Provider
     * 
     * @param customProvider 自定义字节码提供者
     * @param callback       热加载回调（可为null）
     */
    public ZkHotSwapListener(IHotSwapProvider customProvider, IHotSwapCallback callback) {
        this(false, Collections.singletonList(customProvider), callback);
    }

    /**
     * 构造函数 - 使用多个自定义 Provider
     * 
     * @param customProviders 自定义字节码提供者列表
     * @param callback        热加载回调（可为null）
     */
    public ZkHotSwapListener(List<IHotSwapProvider> customProviders, IHotSwapCallback callback) {
        this(false, customProviders, callback);
    }

    /**
     * 完整构造函数
     * 
     * @param useZkBytes      是否使用 ZK 节点数据作为字节码
     * @param customProviders 自定义字节码提供者列表（当 useZkBytes=false 时必须提供）
     * @param callback        热加载回调（可为null）
     */
    public ZkHotSwapListener(boolean useZkBytes, List<IHotSwapProvider> customProviders, IHotSwapCallback callback) {
        this.useZkBytes = useZkBytes;
        this.customProviders = new ArrayList<>(customProviders);
        this.callback = callback;

        if (!useZkBytes && (customProviders == null || customProviders.isEmpty())) {
            throw new IllegalArgumentException("customProviders cannot be null or empty when useZkBytes is false");
        }
    }

    @Override
    public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        ChildData data = event.getData();
        if (data == null) {
            return;
        }

        TreeCacheEvent.Type type = event.getType();
        String path = data.getPath();

        // 只处理节点更新事件
        if (type == TreeCacheEvent.Type.NODE_UPDATED) {
            logger.info("ZK node updated: {}", path);
            handleNodeUpdate(data);
        } else if (type == TreeCacheEvent.Type.NODE_ADDED) {
            logger.info("ZK node added: {}", path);
            handleNodeUpdate(data);
        }
    }

    /**
     * 处理节点更新
     */
    private void handleNodeUpdate(ChildData data) {
        try {
            if (useZkBytes) {
                // 直接使用 ZK 节点的数据作为字节码
                byte[] bytes = data.getData();
                if (bytes != null && bytes.length > 0) {
                    logger.info("Using ZK node data as class bytes, size: {} bytes", bytes.length);
                    HotSwapUtils.hotswapClass(bytes);
                    
                    // 成功回调
                    if (callback != null) {
                        String className = HotSwapUtils.readClassName(bytes);
                        callback.onSuccess(className);
                    }
                } else {
                    logger.warn("ZK node data is empty");
                }
            } else {
                // 使用自定义 Provider 获取字节码
                // 支持多个 Provider，依次执行
                logger.info("Using {} custom provider(s) to get class bytes", customProviders.size());
                
                int successCount = 0;
                int failureCount = 0;
                
                for (int i = 0; i < customProviders.size(); i++) {
                    IHotSwapProvider provider = customProviders.get(i);
                    try {
                        logger.info("Executing provider [{}/{}]", i + 1, customProviders.size());
                        HotSwapUtils.hotswapClass(provider, callback);
                        successCount++;
                    } catch (Exception e) {
                        failureCount++;
                        logger.error("Failed to execute provider [{}/{}]", i + 1, customProviders.size(), e);
                        if (callback != null) {
                            callback.onFailure(null, e);
                        }
                    }
                }
                
                logger.info("Provider execution completed: {} success, {} failure", successCount, failureCount);
            }
        } catch (Exception e) {
            logger.error("Failed to handle ZK node update: {}", data.getPath(), e);
            if (callback != null) {
                callback.onFailure(null, e);
            }
        }
    }

    /**
     * 创建并启动 ZK 监听器
     * 
     * @param client   Curator 客户端
     * @param path     监听的 ZK 路径
     * @param callback 热加载回调
     * @return TreeCache 实例
     */
    public static TreeCache createAndStart(CuratorFramework client, String path, IHotSwapCallback callback) throws Exception {
        ZkHotSwapListener listener = new ZkHotSwapListener(callback);
        TreeCache cache = new TreeCache(client, path);
        cache.getListenable().addListener(listener);
        cache.start();
        logger.info("ZkHotSwapListener started, watching path: {}", path);
        return cache;
    }

    /**
     * 创建并启动 ZK 监听器（使用单个自定义 Provider）
     * 
     * @param client         Curator 客户端
     * @param path           监听的 ZK 路径
     * @param customProvider 自定义字节码提供者
     * @param callback       热加载回调
     * @return TreeCache 实例
     */
    public static TreeCache createAndStart(CuratorFramework client, String path, 
                                          IHotSwapProvider customProvider, IHotSwapCallback callback) throws Exception {
        ZkHotSwapListener listener = new ZkHotSwapListener(customProvider, callback);
        TreeCache cache = new TreeCache(client, path);
        cache.getListenable().addListener(listener);
        cache.start();
        logger.info("ZkHotSwapListener started with custom provider, watching path: {}", path);
        return cache;
    }

    /**
     * 创建并启动 ZK 监听器（使用多个自定义 Provider）
     * 
     * @param client          Curator 客户端
     * @param path            监听的 ZK 路径
     * @param customProviders 自定义字节码提供者列表
     * @param callback        热加载回调
     * @return TreeCache 实例
     */
    public static TreeCache createAndStart(CuratorFramework client, String path, 
                                          List<IHotSwapProvider> customProviders, IHotSwapCallback callback) throws Exception {
        ZkHotSwapListener listener = new ZkHotSwapListener(customProviders, callback);
        TreeCache cache = new TreeCache(client, path);
        cache.getListenable().addListener(listener);
        cache.start();
        logger.info("ZkHotSwapListener started with {} custom provider(s), watching path: {}", customProviders.size(), path);
        return cache;
    }
}

