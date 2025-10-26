package com.zxch.hotswap.example;

import com.zxch.hotswap.IHotSwapCallback;
import com.zxch.hotswap.IHotSwapProvider;
import com.zxch.hotswap.provider.DefaultHotSwapProvider;
import com.zxch.hotswap.zk.ZkHotSwapListener;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.Arrays;
import java.util.List;

/**
 * ZooKeeper 热加载使用示例
 * 
 * @author zhangxun
 */
public class ZkHotSwapExample {

    public static void main(String[] args) throws Exception {
        // 示例1：使用 ZK 节点数据作为字节码
        example1_UseZkBytes();

        // 示例2：使用自定义 Provider
        example2_UseCustomProvider();

        // 示例3：手动创建监听器
        example3_ManualListener();
        
        // 示例5：使用多个 Provider（新功能）
        example5_MultipleProviders();
    }

    /**
     * 示例1：使用 ZK 节点数据作为字节码
     */
    private static void example1_UseZkBytes() throws Exception {
        System.out.println("=== 示例1：使用 ZK 节点数据作为字节码 ===");

        // 创建 ZK 客户端
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();

        // 创建回调
        IHotSwapCallback callback = new IHotSwapCallback() {
            @Override
            public void onSuccess(String className) {
                System.out.println("✓ ZK 热加载成功: " + className);
            }

            @Override
            public void onFailure(String className, Throwable throwable) {
                System.err.println("✗ ZK 热加载失败: " + className);
                throwable.printStackTrace();
            }
        };

        // 创建并启动监听器（直接使用 ZK 节点的 data 作为字节码）
        TreeCache cache = ZkHotSwapListener.createAndStart(
                client,
                "/hotswap/classes",  // 监听的 ZK 路径
                callback
        );

        System.out.println("监听器已启动，等待 ZK 节点变化...");
        System.out.println("在 ZK 中更新 /hotswap/classes 节点的数据（class 字节码）即可触发热加载");

        // 保持运行
        Thread.sleep(Long.MAX_VALUE);

        // 清理资源
        cache.close();
        client.close();
    }

    /**
     * 示例2：使用自定义 Provider
     */
    private static void example2_UseCustomProvider() throws Exception {
        System.out.println("\n=== 示例2：使用自定义 Provider ===");

        // 创建 ZK 客户端
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();

        // 自定义 Provider（从本地文件读取）
        IHotSwapProvider provider = new DefaultHotSwapProvider("target/classes/com/example/MyClass.class");

        // 创建回调
        IHotSwapCallback callback = new IHotSwapCallback() {
            @Override
            public void onSuccess(String className) {
                System.out.println("✓ 热加载成功: " + className);
            }

            @Override
            public void onFailure(String className, Throwable throwable) {
                System.err.println("✗ 热加载失败: " + className);
            }
        };

        // 创建并启动监听器（使用自定义 Provider）
        TreeCache cache = ZkHotSwapListener.createAndStart(
                client,
                "/hotswap/trigger",  // 监听的 ZK 路径
                provider,            // 自定义 Provider
                callback
        );

        System.out.println("监听器已启动（使用自定义 Provider）");
        System.out.println("在 ZK 中更新 /hotswap/trigger 节点即可触发热加载");
        System.out.println("字节码将从本地文件读取，而不是 ZK 节点数据");

        // 保持运行
        Thread.sleep(Long.MAX_VALUE);

        // 清理资源
        cache.close();
        client.close();
    }

    /**
     * 示例3：手动创建监听器
     */
    private static void example3_ManualListener() throws Exception {
        System.out.println("\n=== 示例3：手动创建监听器 ===");

        // 创建 ZK 客户端
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();

        // 创建回调
        IHotSwapCallback callback = new IHotSwapCallback() {
            private int count = 0;

            @Override
            public void onSuccess(String className) {
                count++;
                System.out.println(String.format("[%d] ✓ 热加载成功: %s", count, className));
            }

            @Override
            public void onFailure(String className, Throwable throwable) {
                System.err.println("✗ 热加载失败: " + className);
            }
        };

        // 方式1：使用 ZK 字节码
        ZkHotSwapListener listener1 = new ZkHotSwapListener(callback);

        // 方式2：使用自定义 Provider
        IHotSwapProvider provider = new DefaultHotSwapProvider("target/classes/com/example/MyClass.class");
        ZkHotSwapListener listener2 = new ZkHotSwapListener(provider, callback);

        // 方式3：完整构造函数
        ZkHotSwapListener listener3 = new ZkHotSwapListener(
                true,      // useZkBytes
                null,      // customProvider
                callback   // callback
        );

        // 创建 TreeCache 并添加监听器
        TreeCache cache = new TreeCache(client, "/hotswap/classes");
        cache.getListenable().addListener(listener1);
        cache.start();

        System.out.println("手动创建的监听器已启动");

        // 保持运行
        Thread.sleep(Long.MAX_VALUE);

        // 清理资源
        cache.close();
        client.close();
    }

    /**
     * 示例4：从远程服务器获取字节码
     */
    public static void example4_RemoteProvider() throws Exception {
        System.out.println("\n=== 示例4：从远程服务器获取字节码 ===");

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();

        // 自定义 Provider - 从远程服务器下载
        IHotSwapProvider remoteProvider = new IHotSwapProvider() {
            @Override
            public byte[] getClassBytes() {
                // 从远程服务器下载字节码
                System.out.println("从远程服务器下载字节码...");
                // return HttpClient.download("http://server/classes/MyClass.class");
                return null;
            }
        };

        IHotSwapCallback callback = new IHotSwapCallback() {
            @Override
            public void onSuccess(String className) {
                System.out.println("✓ 远程热加载成功: " + className);
            }

            @Override
            public void onFailure(String className, Throwable throwable) {
                System.err.println("✗ 远程热加载失败");
            }
        };

        // ZK 节点变化时，从远程服务器获取字节码并热加载
        TreeCache cache = ZkHotSwapListener.createAndStart(
                client,
                "/hotswap/remote-trigger",
                remoteProvider,
                callback
        );

        System.out.println("远程 Provider 监听器已启动");

        Thread.sleep(Long.MAX_VALUE);
        cache.close();
        client.close();
    }

    /**
     * 示例5：使用多个 Provider（新功能）
     * 一个 ZK 节点变更，可以触发多个 Provider 依次执行
     */
    public static void example5_MultipleProviders() throws Exception {
        System.out.println("\n=== 示例5：使用多个 Provider ===");

        // 创建 ZK 客户端
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();

        // 创建多个 Provider
        IHotSwapProvider provider1 = new DefaultHotSwapProvider("target/classes/com/example/ClassA.class");
        IHotSwapProvider provider2 = new DefaultHotSwapProvider("target/classes/com/example/ClassB.class");
        IHotSwapProvider provider3 = new DefaultHotSwapProvider("target/classes/com/example/ClassC.class");

        // 也可以使用自定义的 Provider
        IHotSwapProvider customProvider = new IHotSwapProvider() {
            @Override
            public byte[] getClassBytes() {
                System.out.println("自定义 Provider 执行：从远程服务器获取字节码");
                // 实际场景可以从远程服务器、数据库等获取
                return null;
            }
        };

        // 将多个 Provider 放入列表
        List<IHotSwapProvider> providers = Arrays.asList(
                provider1,
                provider2,
                provider3,
                customProvider
        );

        // 创建回调
        IHotSwapCallback callback = new IHotSwapCallback() {
            @Override
            public void onSuccess(String className) {
                System.out.println("✓ 热加载成功: " + className);
            }

            @Override
            public void onFailure(String className, Throwable throwable) {
                System.err.println("✗ 热加载失败: " + className);
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }
        };

        // 创建并启动监听器（使用多个 Provider）
        TreeCache cache = ZkHotSwapListener.createAndStart(
                client,
                "/hotswap/multi-trigger",  // 监听的 ZK 路径
                providers,                  // 多个 Provider
                callback
        );

        System.out.println("多 Provider 监听器已启动");
        System.out.println("当 ZK 节点 /hotswap/multi-trigger 变更时，将依次执行 " + providers.size() + " 个 Provider");
        System.out.println("每个 Provider 会独立执行热加载，互不影响");

        // 保持运行
        Thread.sleep(Long.MAX_VALUE);

        // 清理资源
        cache.close();
        client.close();
    }

    /**
     * 示例6：手动创建多 Provider 监听器
     */
    public static void example6_ManualMultipleProviders() throws Exception {
        System.out.println("\n=== 示例6：手动创建多 Provider 监听器 ===");

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();

        // 创建多个 Provider
        List<IHotSwapProvider> providers = Arrays.asList(
                new DefaultHotSwapProvider("target/classes/com/example/Service1.class"),
                new DefaultHotSwapProvider("target/classes/com/example/Service2.class"),
                new DefaultHotSwapProvider("target/classes/com/example/Service3.class")
        );

        // 创建回调
        IHotSwapCallback callback = new IHotSwapCallback() {
            private int successCount = 0;
            private int failureCount = 0;

            @Override
            public void onSuccess(String className) {
                successCount++;
                System.out.println(String.format("[成功 %d] ✓ %s", successCount, className));
            }

            @Override
            public void onFailure(String className, Throwable throwable) {
                failureCount++;
                System.err.println(String.format("[失败 %d] ✗ %s", failureCount, className));
            }
        };

        // 手动创建监听器
        ZkHotSwapListener listener = new ZkHotSwapListener(providers, callback);

        // 创建 TreeCache 并添加监听器
        TreeCache cache = new TreeCache(client, "/hotswap/services");
        cache.getListenable().addListener(listener);
        cache.start();

        System.out.println("手动创建的多 Provider 监听器已启动");
        System.out.println("监听路径: /hotswap/services");
        System.out.println("Provider 数量: " + providers.size());

        // 保持运行
        Thread.sleep(Long.MAX_VALUE);

        // 清理资源
        cache.close();
        client.close();
    }
}

