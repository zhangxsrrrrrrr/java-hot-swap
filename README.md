# Hot-Swap - Java 热加载工具包

借助zk实现分布式代码热更

## 特性

- ✅ **极简设计** - 核心只有一个工具类，开箱即用
- ✅ **零依赖** - 只依赖 ByteBuddy，无其他第三方依赖
- ✅ **灵活扩展** - 提供接口支持自定义字节码获取方式
- ✅ **高版本JDK支持** - 基于 ByteBuddy，完美支持高版本 JDK
- ✅ **线程安全** - 热加载操作使用同步控制

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>hot-swap</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 使用 Provider 热加载（推荐）

最简单的方式，创建 Provider 即可：

```java
import com.zfoo.hotswap.IHotSwapProvider;
import com.zfoo.hotswap.provider.DefaultHotSwapProvider;
import com.zfoo.hotswap.util.HotSwapUtils;

// 创建 Provider（指定要热加载的文件）
IHotSwapProvider provider = new DefaultHotSwapProvider("target/classes/com/example/MyClass.class");

// 一行代码完成热加载！
HotSwapUtils.hotswapClass(provider);

// 批量热加载
HotSwapUtils.hotswapClasses(
    new DefaultHotSwapProvider("target/classes/com/example/Service1.class"),
    new DefaultHotSwapProvider("target/classes/com/example/Service2.class"),
    new DefaultHotSwapProvider("target/classes/com/example/Service3.class")
);
```

### 3. 直接使用字节码（可选）

如果你已经有字节码：

```java
import com.zfoo.hotswap.util.HotSwapUtils;
import java.nio.file.Files;

byte[] bytes = Files.readAllBytes(classFile.toPath());
HotSwapUtils.hotswapClass(bytes);
```

### 4. 自定义 Provider

实现自己的字节码获取逻辑：

```java
// 从远程服务器获取
IHotSwapProvider remoteProvider = new IHotSwapProvider() {
    @Override
    public byte[] getClassBytes() {
        return downloadFromServer("com.example.MyClass");
    }
};

// 从加密文件读取
IHotSwapProvider encryptedProvider = new IHotSwapProvider() {
    @Override
    public byte[] getClassBytes() {
        byte[] encrypted = Files.readAllBytes(Paths.get("encrypted.class"));
        return decrypt(encrypted);
    }
};

// 使用自定义 Provider
HotSwapUtils.hotswapClass(remoteProvider);
```

## ZooKeeper 集成

### ZkHotSwapListener

ZooKeeper 热加载监听器，监听 ZK 节点变化自动触发热加载。

**两种模式：**

1. **使用 ZK 节点数据作为字节码**（直接模式）
```java
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

// 创建并启动监听器
TreeCache cache = ZkHotSwapListener.createAndStart(
    client,                  // Curator 客户端
    "/hotswap/classes",      // 监听的 ZK 路径
    callback                 // 回调
);

// 在 ZK 中更新节点数据（class 字节码）即可触发热加载
```

2. **使用自定义 Provider**（间接模式）
```java
// 自定义 Provider
IHotSwapProvider provider = new DefaultHotSwapProvider("target/classes/MyClass.class");

// 创建并启动监听器
TreeCache cache = ZkHotSwapListener.createAndStart(
    client,                  // Curator 客户端
    "/hotswap/trigger",      // 监听的 ZK 路径
    provider,                // 自定义 Provider
    callback                 // 回调
);

// 在 ZK 中更新节点（任意数据）即可触发热加载
// 字节码从 Provider 获取，而不是 ZK 节点数据
```

**使用场景：**
- 模式1：适合字节码存储在 ZK 中的场景
- 模式2：适合 ZK 只作为触发器，字节码从其他地方获取的场景

## 核心组件

### HotSwapUtils

核心工具类，提供热加载功能。

**主要方法：**
- `hotswapClass(IHotSwapProvider provider)` - 使用 Provider 热加载
- `hotswapClass(IHotSwapProvider provider, IHotSwapCallback callback)` - 带回调的热加载
- `hotswapClasses(IHotSwapProvider... providers)` - 批量热加载
- `hotswapClasses(IHotSwapCallback callback, IHotSwapProvider... providers)` - 带回调的批量热加载
- `hotswapClass(byte[] bytes)` - 直接使用字节码热加载
- `readClassName(byte[] bytes)` - 从字节码中读取类名

### IHotSwapProvider

字节码提供者接口，用于自定义字节码获取方式。

**方法：**
- `byte[] getClassBytes()` - 获取字节码

### IHotSwapCallback

热加载回调接口，用于接收热加载结果通知。

**方法：**
- `onSuccess(String className)` - 热加载成功回调
- `onFailure(String className, Throwable throwable)` - 热加载失败回调

### DefaultHotSwapProvider

默认实现，从本地文件系统读取字节码。

### HotSwapContext

线程安全的上下文对象，可用于存储自定义数据。

**主要方法：**
- `setAttr(String, Object)` - 设置属性
- `getAttr(String)` - 获取属性
- `getAttr(String, Class<T>)` - 获取指定类型的属性
- `removeAttr(String)` - 移除属性
- `clear()` - 清空所有属性

## 使用示例

### 示例1：最简单的用法

```java
// 创建 Provider（指定要热加载的文件）
IHotSwapProvider provider = new DefaultHotSwapProvider("target/classes/com/example/MyClass.class");

// 一行代码完成热加载
HotSwapUtils.hotswapClass(provider);
```

### 示例2：使用回调（推荐）

```java
IHotSwapProvider provider = new DefaultHotSwapProvider("target/classes/com/example/MyClass.class");

// 创建回调
IHotSwapCallback callback = new IHotSwapCallback() {
    @Override
    public void onSuccess(String className) {
        System.out.println("✓ 热加载成功: " + className);
        // 清理缓存、发送通知等
    }

    @Override
    public void onFailure(String className, Throwable throwable) {
        System.err.println("✗ 热加载失败: " + className);
        // 记录日志、发送告警等
    }
};

// 使用回调进行热加载
HotSwapUtils.hotswapClass(provider, callback);
```

### 示例3：批量热加载

```java
// 创建统计回调
IHotSwapCallback callback = new IHotSwapCallback() {
    private int successCount = 0;
    
    @Override
    public void onSuccess(String className) {
        System.out.println("[" + (++successCount) + "] ✓ " + className);
    }
    
    @Override
    public void onFailure(String className, Throwable throwable) {
        System.err.println("✗ " + className + " - " + throwable.getMessage());
    }
};

// 批量热加载（带回调）
HotSwapUtils.hotswapClasses(
    callback,
    new DefaultHotSwapProvider("target/classes/com/example/Service1.class"),
    new DefaultHotSwapProvider("target/classes/com/example/Service2.class"),
    new DefaultHotSwapProvider("target/classes/com/example/Service3.class")
);
```

### 示例4：自定义 Provider

```java
// 从远程服务器获取字节码
IHotSwapProvider remoteProvider = new IHotSwapProvider() {
    @Override
    public byte[] getClassBytes() {
        return downloadFromServer("com.example.MyClass");
    }
};

// 从加密文件读取
IHotSwapProvider encryptedProvider = new IHotSwapProvider() {
    @Override
    public byte[] getClassBytes() {
        byte[] encrypted = Files.readAllBytes(Paths.get("encrypted.class"));
        return decrypt(encrypted);
    }
};

// 使用自定义 Provider（带回调）
HotSwapUtils.hotswapClass(remoteProvider, callback);
```

### 示例5：直接使用字节码

```java
// 如果你已经有字节码
byte[] bytes = Files.readAllBytes(classFile.toPath());
HotSwapUtils.hotswapClass(bytes);
```

更多示例请参考 `HotSwapExample.java`。

## 工作原理

1. **字节码读取**：从文件、网络或其他来源获取 class 字节码
2. **类名解析**：使用 ASM 解析字节码，获取类的全限定名
3. **类加载检查**：检查类是否已在当前 JVM 中加载
4. **热加载执行**：使用 ByteBuddy Agent 的 `redefineClasses()` 方法重定义类

## 注意事项

### JVM 热加载限制

1. **只能修改方法体**：不能添加/删除字段或方法，不能修改类继承关系
2. **静态变量保持不变**：热加载后静态变量的值不会重置
3. **已创建的对象**：不会自动更新，只影响新创建的对象
4. **内联优化**：JIT 编译器可能已经内联了方法，需要禁用优化

### 使用建议

1. **仅用于开发环境**：不建议在生产环境使用
2. **配合 IDE 使用**：修改代码后自动编译，然后手动触发热加载
3. **清理缓存**：热加载后需要手动清理相关缓存

## 常见问题

### Q: 为什么热加载后没有生效？

A: 可能的原因：
1. 修改了类结构（字段、方法签名），JVM 不支持
2. 类已经被 JIT 内联优化，需要禁用优化
3. 使用了缓存的对象，需要清理缓存

### Q: 可以在生产环境使用吗？

A: 不建议。这是一个开发工具，生产环境应该通过正常的部署流程更新代码。

### Q: 支持哪些 JDK 版本？

A: 支持 JDK 11 及以上版本，基于 ByteBuddy 实现。

### Q: 如何集成到我的项目中？

A: 这是一个纯工具包，不提供自动监控功能。你需要自己实现文件监控逻辑，然后调用 `HotSwapUtils.hotswapClass()` 进行热加载。

## 技术栈

- **ByteBuddy** - 字节码操作和类重定义
- **ASM** - 字节码解析（ByteBuddy 内置）
- **SLF4J** - 日志框架

## 项目结构

```
hot-swap/
├── IHotSwapProvider.java          # 字节码提供者接口
├── HotSwapContext.java            # 上下文对象
├── util/
│   └── HotSwapUtils.java          # 核心工具类
├── provider/
│   └── DefaultHotSwapProvider.java # 默认实现
└── example/
    └── HotSwapExample.java        # 使用示例
```

## 许可证

Apache License 2.0

## 作者

zhangxun

## 贡献

欢迎提交 Issue 和 Pull Request！

