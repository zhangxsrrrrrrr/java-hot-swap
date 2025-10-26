package com.zxch.hotswap.util;


import com.zxch.hotswap.IHotSwapCallback;
import com.zxch.hotswap.IHotSwapProvider;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.jar.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassDefinition;

/**
 * Hotswap java class
 * 去除java-javassist的依赖，只是应用ByteBuddy，更好的去适配高版本的jdk
 * @author zhangxun
 */
public abstract class HotSwapUtils {

    private static final Logger logger = LoggerFactory.getLogger(HotSwapUtils.class);

    private static void hotswapClassByJavassist(Class<?> clazz, byte[] bytes) {
        hotswapClassByByteBuddy(clazz, bytes);
    }

    public static synchronized void hotswapClass(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        String clazzName = readClassName(bytes);

        Class<?> clazz = null;
        try {
            clazz = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            logger.error("The class:[{}] could not be found in the current project and ignore this hot update", clazzName);
            return;
        }

        hotswapClassByJavassist(clazz, bytes);
    }


    private static void hotswapClassByByteBuddy(Class<?> clazz, byte[] bytes) {
        try {
            logger.info("ByteBuddy hot update class:[{}] started", clazz.getName());
            // Byte Buddy hot update
            java.lang.instrument.Instrumentation instrumentation = ByteBuddyAgent.install();
            instrumentation.redefineClasses(new ClassDefinition(clazz, bytes));
            logger.info("ByteBuddy hot update class:[{}] succeeded", clazz.getName());
        } catch (Throwable t) {
            logger.error("ByteBuddy hot update class:[{}] failed", clazz.getName());
        }
    }

    /**
     * 使用 Provider 热加载
     * 
     * @param provider 字节码提供者
     */
    public static synchronized void hotswapClass(IHotSwapProvider provider) {
        hotswapClass(provider, null);
    }

    /**
     * 使用 Provider 热加载，并提供回调
     * 
     * @param provider 字节码提供者
     * @param callback 热加载回调（可为null）
     */
    public static synchronized void hotswapClass(IHotSwapProvider provider, IHotSwapCallback callback) {
        if (provider == null) {
            logger.error("Provider cannot be null");
            if (callback != null) {
                callback.onFailure(null, new IllegalArgumentException("Provider cannot be null"));
            }
            return;
        }
        
        String className = null;
        try {
            // 使用 provider 获取字节码
            byte[] bytes = provider.getClassBytes();
            if (bytes == null || bytes.length == 0) {
                logger.error("Failed to get bytes from provider");
                if (callback != null) {
                    callback.onFailure(null, new RuntimeException("Failed to get bytes from provider"));
                }
                return;
            }
            
            // 读取类名
            className = readClassName(bytes);
            
            // 执行热加载
            hotswapClass(bytes);
            
            // 成功回调
            if (callback != null) {
                callback.onSuccess(className);
            }
            
        } catch (Exception e) {
            logger.error("Hot swap failed", e);
            if (callback != null) {
                callback.onFailure(className, e);
            }
        }
    }

    /**
     * 批量热加载多个 Provider
     * 
     * @param providers 字节码提供者列表
     */
    public static synchronized void hotswapClasses(IHotSwapProvider... providers) {
        hotswapClasses(null, providers);
    }

    /**
     * 批量热加载多个 Provider，并提供回调
     * 
     * @param callback  热加载回调（可为null）
     * @param providers 字节码提供者列表
     */
    public static synchronized void hotswapClasses(IHotSwapCallback callback, IHotSwapProvider... providers) {
        if (providers == null || providers.length == 0) {
            logger.warn("No providers to hot swap");
            return;
        }
        
        for (IHotSwapProvider provider : providers) {
            if (provider != null) {
                hotswapClass(provider, callback);
            }
        }
    }

    /**
     * 使用ASM读取类名（ByteBuddy内部已包含ASM依赖）
     * 相比自定义ClassFile解析，ASM更高效且无需手动管理流
     */
    public static String readClassName(byte[] bytes) {
        try {
            // 使用ASM的ClassReader解析字节码
            ClassReader classReader = new ClassReader(bytes);
            // getClassName返回的是内部格式（com/example/MyClass），需要转换为标准格式
            return classReader.getClassName().replace('/', '.');
        } catch (Exception e) {
            throw new RuntimeException("Failed to read class name from bytes", e);
        }
    }

}
