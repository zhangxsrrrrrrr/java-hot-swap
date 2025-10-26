package com.zxch.hotswap.provider;

import com.zxch.hotswap.IHotSwapProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 默认的热加载提供者实现
 * <p>
 * 提供基于本地文件系统的简单实现
 * 
 * @author zhangxun
 */
public class DefaultHotSwapProvider implements IHotSwapProvider {

    private static final Logger logger = LoggerFactory.getLogger(DefaultHotSwapProvider.class);

    private final File classFile;

    /**
     * 构造函数
     * 
     * @param classFile 需要热加载的 class 文件
     */
    public DefaultHotSwapProvider(File classFile) {
        this.classFile = classFile;
    }

    /**
     * 构造函数
     * 
     * @param classFilePath 需要热加载的 class 文件路径
     */
    public DefaultHotSwapProvider(String classFilePath) {
        this.classFile = new File(classFilePath);
    }

    @Override
    public byte[] getClassBytes() {
        if (classFile == null || !classFile.exists()) {
            logger.warn("Class file does not exist: {}", classFile);
            return null;
        }

        try {
            return Files.readAllBytes(classFile.toPath());
        } catch (IOException e) {
            logger.error("Failed to read class file: {}", classFile, e);
            return null;
        }
    }
}

