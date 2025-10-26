package com.zxch.hotswap;

/**
 * 热加载提供者接口 - 提供获取字节码的方式
 * <p>
 * 这是一个简单的工具接口，用于获取class文件的字节码
 * 
 * @author zhangxun
 */
public interface IHotSwapProvider {

    /**
     * 获取需要热加载的class字节码
     * <p>
     * 实现此方法来提供字节码，可以从任何来源获取：
     * - 本地文件系统
     * - 远程服务器
     * - 数据库
     * - 内存缓存
     * 
     * @return class文件的字节码，返回null表示无法读取
     */
    byte[] getClassBytes();
}

