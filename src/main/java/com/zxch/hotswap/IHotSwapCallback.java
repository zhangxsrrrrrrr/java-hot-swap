package com.zxch.hotswap;

/**
 * 热加载回调接口
 * 
 * @author zhangxun
 */
public interface IHotSwapCallback {

    /**
     * 热加载成功回调
     * 
     * @param className 被热加载的类名
     */
    void onSuccess(String className);

    /**
     * 热加载失败回调
     * 
     * @param className 热加载失败的类名（可能为null）
     * @param throwable 异常信息
     */
    void onFailure(String className, Throwable throwable);
}

