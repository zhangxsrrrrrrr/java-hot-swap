package com.zxch.hotswap.example;


import com.zxch.hotswap.IHotSwapCallback;
import com.zxch.hotswap.IHotSwapProvider;
import com.zxch.hotswap.provider.DefaultHotSwapProvider;
import com.zxch.hotswap.util.HotSwapUtils;

import java.io.File;

/**
 * 热加载工具使用示例
 * 
 * @author zhangxun
 */
public class HotSwapExample {

    public static void main(String[] args) {
        // 示例1：直接使用字节码热加载
        example1_DirectHotSwap();

        // 示例2：使用 Provider 热加载（推荐）
        example2_UseProvider();

        // 示例3：使用回调
        example3_UseCallback();

        // 示例4：批量热加载
        example4_BatchHotSwap();

        // 示例5：自定义 Provider
        example5_CustomProvider();
    }

    /**
     * 示例1：直接使用字节码热加载
     */
    private static void example1_DirectHotSwap() {
        System.out.println("=== 示例1：直接使用字节码 ===");

        // 如果你已经有字节码，可以直接调用
        File classFile = new File("target/classes/com/example/MyClass.class");
        
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(classFile.toPath());
            HotSwapUtils.hotswapClass(bytes);
            System.out.println("热加载成功！");
        } catch (Exception e) {
            System.err.println("热加载失败: " + e.getMessage());
        }
    }

    /**
     * 示例2：使用 Provider 热加载（推荐）
     */
    private static void example2_UseProvider() {
        System.out.println("\n=== 示例2：使用 Provider（推荐） ===");

        // 创建 Provider（指定要热加载的文件）
        IHotSwapProvider provider = new DefaultHotSwapProvider("target/classes/com/example/MyClass.class");

        // 一行代码完成热加载！
        HotSwapUtils.hotswapClass(provider);
        
        System.out.println("热加载完成！");
    }

    /**
     * 示例3：使用回调
     */
    private static void example3_UseCallback() {
        System.out.println("\n=== 示例3：使用回调 ===");

        IHotSwapProvider provider = new DefaultHotSwapProvider("target/classes/com/example/MyClass.class");

        // 创建回调
        IHotSwapCallback callback = new IHotSwapCallback() {
            @Override
            public void onSuccess(String className) {
                System.out.println("✓ 热加载成功: " + className);
                
                // 可以在这里做一些后续处理
                // 例如：清理缓存、发送通知、更新统计等
            }

            @Override
            public void onFailure(String className, Throwable throwable) {
                System.err.println("✗ 热加载失败: " + className);
                System.err.println("  原因: " + throwable.getMessage());
                
                // 可以在这里做一些错误处理
                // 例如：记录日志、发送告警、回滚操作等
            }
        };

        // 使用回调进行热加载
        HotSwapUtils.hotswapClass(provider, callback);
    }

    /**
     * 示例4：批量热加载
     */
    private static void example4_BatchHotSwap() {
        System.out.println("\n=== 示例4：批量热加载 ===");

        // 创建统计回调
        IHotSwapCallback callback = new IHotSwapCallback() {
            private int successCount = 0;
            private int failureCount = 0;

            @Override
            public void onSuccess(String className) {
                successCount++;
                System.out.println("  [" + successCount + "] ✓ " + className);
            }

            @Override
            public void onFailure(String className, Throwable throwable) {
                failureCount++;
                System.err.println("  [" + failureCount + "] ✗ " + className + " - " + throwable.getMessage());
            }
        };
        
        // 批量热加载（带回调）
        HotSwapUtils.hotswapClasses(
            callback,
            new DefaultHotSwapProvider("target/classes/com/example/Service1.class"),
            new DefaultHotSwapProvider("target/classes/com/example/Service2.class"),
            new DefaultHotSwapProvider("target/classes/com/example/Service3.class")
        );
        
        System.out.println("批量热加载完成！");
    }

    /**
     * 示例5：自定义 Provider
     */
    private static void example5_CustomProvider() {
        System.out.println("\n=== 示例5：自定义 Provider ===");

        // 自定义 Provider，可以从任何地方获取字节码
        IHotSwapProvider customProvider = new IHotSwapProvider() {
            @Override
            public byte[] getClassBytes() {
                try {
                    System.out.println("正在读取自定义来源的字节码...");
                    
                    // 可以在这里添加自定义逻辑
                    // 例如：从远程服务器下载、从数据库读取、解密等
                    File file = new File("target/classes/com/example/MyClass.class");
                    byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                    
                    // 可以对字节码进行处理
                    // bytes = decrypt(bytes);
                    // bytes = transform(bytes);
                    
                    return bytes;
                } catch (Exception e) {
                    System.err.println("读取失败: " + e.getMessage());
                    return null;
                }
            }
        };

        HotSwapUtils.hotswapClass(customProvider);
        
        System.out.println("自定义 Provider 热加载完成！");
    }

    /**
     * 示例6：从远程服务器热加载（示例）
     */
    public static void example6_RemoteHotSwap() {
        System.out.println("\n=== 示例6：从远程服务器热加载 ===");

        IHotSwapProvider remoteProvider = new IHotSwapProvider() {
            @Override
            public byte[] getClassBytes() {
                // 从远程服务器下载字节码
                String className = "com.example.MyClass";
                return downloadFromServer(className);
            }
            
            private byte[] downloadFromServer(String className) {
                // 实现从远程服务器下载的逻辑
                // HttpClient client = ...
                // return client.download("http://server/classes/" + className);
                System.out.println("从远程服务器下载: " + className);
                return null;
            }
        };

        HotSwapUtils.hotswapClass(remoteProvider);
    }
}

