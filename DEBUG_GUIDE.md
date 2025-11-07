# BigClean Scanner 插件调试指南

## 🚀 快速开始调试

### 1. 启动调试实例

```bash
# 方法一：普通启动（用于UI测试）
./gradlew runIde

# 方法二：调试模式启动（用于断点调试）
./gradlew runIde --debug-jvm

# 方法三：指定调试端口
./gradlew runIde -Dorg.gradle.debug=true --debug-jvm
```

### 2. 在IDEA中设置断点调试

1. 在你的Java代码中设置断点
2. 启动调试实例：`./gradlew runIde --debug-jvm`
3. 在调试实例中触发插件功能
4. 回到开发IDEA查看断点信息

## 🔧 调试配置

### Gradle配置

在 `build.gradle` 中添加调试配置：

```gradle
runIde {
    // 启用调试模式
    jvmArgs = [
        '-Xdebug',
        '-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005',
        '-Xms512m',
        '-Xmx2048m'
    ]
    
    // 设置系统属性
    systemProperties = [
        'idea.debug.mode': 'true',
        'idea.log.debug.categories': 'com.hello.bigclean'
    ]
}
```

### 远程调试配置

1. 在IDEA中创建远程调试配置：
   - `Run` → `Edit Configurations`
   - 点击 `+` → `Remote JVM Debug`
   - 设置：
     - Name: `BigClean Plugin Debug`
     - Host: `localhost`
     - Port: `5005`

## 📝 日志调试

### 1. 使用IDEA日志系统

```java
import com.intellij.openapi.diagnostic.Logger;

public class YourClass {
    private static final Logger LOG = Logger.getInstance(YourClass.class);
    
    public void yourMethod() {
        LOG.info("信息日志: 方法开始执行");
        LOG.debug("调试日志: 详细信息");
        LOG.warn("警告日志: 注意事项");
        LOG.error("错误日志: 异常信息", exception);
    }
}
```

### 2. 查看日志文件

日志文件位置：
- **macOS**: `~/Library/Logs/JetBrains/IntelliJIdea{version}/idea.log`
- **Windows**: `%APPDATA%\JetBrains\IntelliJIdea{version}\log\idea.log`
- **Linux**: `~/.cache/JetBrains/IntelliJIdea{version}/log/idea.log`

### 3. 实时查看日志

```bash
# macOS/Linux
tail -f ~/Library/Logs/JetBrains/IntelliJIdea*/idea.log

# 过滤特定内容
tail -f ~/Library/Logs/JetBrains/IntelliJIdea*/idea.log | grep "bigclean"
```

## 🎯 关键调试点

### 1. 插件加载调试

在 `ClassPathAnalyzerFactory` 中已添加调试输出：

```java
// 静态初始化块 - 插件加载时执行
static {
    System.out.println("=== ClassPathAnalyzerFactory 类被加载 ===");
}

// 构造函数 - 创建实例时执行
public ClassPathAnalyzerFactory() {
    System.out.println("=== ClassPathAnalyzerFactory 构造函数被调用 ===");
}
```

### 2. 工具窗口创建调试

```java
@Override
public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    System.out.println("=== createToolWindowContent() 被调用 ===");
    System.out.println("项目名称: " + project.getName());
    System.out.println("项目路径: " + project.getBasePath());
    // ... 设置断点在这里
}
```

### 3. 分析功能调试

每个分析方法都已添加详细日志：

```java
private void refreshRegexAnalysis() {
    System.out.println("=== 开始正则表达式分析 ===");
    // ... 设置断点在这里调试分析逻辑
}
```

## 🛠️ 常见调试场景

### 场景1: 插件无法加载

**症状**: 插件不出现在工具窗口中

**调试步骤**:
1. 检查 `plugin.xml` 配置
2. 查看控制台输出是否有类加载信息
3. 检查依赖是否正确

```bash
# 查看插件构建信息
./gradlew printBuildInfo

# 重新构建插件
./gradlew clean buildPlugin
```

### 场景2: 分析功能不工作

**症状**: 点击刷新按钮没有反应

**调试步骤**:
1. 在分析方法开头设置断点
2. 检查 `currentProject` 是否为null
3. 查看异常堆栈信息

```java
// 在这些位置设置断点
private void refreshRegexAnalysis() {
    System.out.println("=== 开始正则表达式分析 ==="); // 断点1
    if (currentProject == null) {
        System.out.println("ERROR: currentProject 为 null"); // 断点2
        return;
    }
    // ...
}
```

### 场景3: Spoon分析失败

**症状**: Spoon AST分析报错

**调试步骤**:
1. 检查项目类路径设置
2. 验证Java源文件路径
3. 查看Spoon相关异常

```java
// 在SpoonUnusedClassAnalyzer中添加调试
public static DefaultTreeModel buildUnusedClassTreeSpoon(Project project) {
    System.out.println("Spoon分析开始，项目路径: " + project.getBasePath());
    // 设置断点在这里
}
```

### 场景4: UI界面问题

**症状**: 界面显示异常或无响应

**调试步骤**:
1. 检查Swing组件创建过程
2. 验证事件监听器
3. 查看UI线程相关问题

```java
// 在UI创建方法中设置断点
private JPanel createTabPanelWithRefreshButton(...) {
    System.out.println("创建UI面板: " + tabName); // 断点
    // ...
}
```

## 🔍 高级调试技巧

### 1. 条件断点

在IDEA中设置条件断点：
- 右键断点 → `More` → 添加条件
- 例如：`project.getName().equals("target-project")`

### 2. 异常断点

设置异常断点捕获特定异常：
- `Run` → `View Breakpoints` → `+` → `Java Exception Breakpoints`
- 添加你关心的异常类型

### 3. 方法断点

在方法签名上设置断点，可以捕获方法的进入和退出：
- 在方法名行号处设置断点
- 右键选择 `Method entry` 或 `Method exit`

### 4. 字段监视断点

监视字段值的变化：
- 在字段声明处设置断点
- 选择 `Field access` 或 `Field modification`

## 📊 性能调试

### 1. 内存使用监控

```java
// 在分析开始前后记录内存使用
Runtime runtime = Runtime.getRuntime();
long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
System.out.println("分析前内存使用: " + beforeMemory / 1024 / 1024 + " MB");

// ... 执行分析 ...

long afterMemory = runtime.totalMemory() - runtime.freeMemory();
System.out.println("分析后内存使用: " + afterMemory / 1024 / 1024 + " MB");
System.out.println("内存增长: " + (afterMemory - beforeMemory) / 1024 / 1024 + " MB");
```

### 2. 执行时间监控

```java
// 已在代码中实现
long startTime = System.currentTimeMillis();
// ... 执行操作 ...
long endTime = System.currentTimeMillis();
System.out.println("执行耗时: " + (endTime - startTime) + "ms");
```

## 🚨 故障排除

### 常见问题及解决方案

1. **ClassNotFoundException**
   - 检查依赖配置
   - 验证类路径设置
   - 重新构建项目

2. **NullPointerException**
   - 检查对象初始化
   - 验证方法调用顺序
   - 添加空值检查

3. **UI无响应**
   - 检查是否在EDT线程中执行UI操作
   - 使用 `SwingUtilities.invokeLater()`
   - 避免在UI线程中执行耗时操作

4. **插件安装失败**
   - 检查IDEA版本兼容性
   - 验证插件包完整性
   - 查看IDEA错误日志

## 📝 调试检查清单

- [ ] 插件是否正确加载？
- [ ] 工具窗口是否正常显示？
- [ ] 项目对象是否正确传递？
- [ ] 分析方法是否被调用？
- [ ] 异常是否被正确处理？
- [ ] UI组件是否正确创建？
- [ ] 日志输出是否正常？
- [ ] 内存使用是否合理？

## 🔧 调试工具推荐

1. **IDEA内置调试器**: 最主要的调试工具
2. **JProfiler**: 性能分析工具
3. **VisualVM**: JVM监控工具
4. **JConsole**: JMX监控工具
5. **MAT (Memory Analyzer Tool)**: 内存分析工具

---

**提示**: 调试时建议先从简单的日志输出开始，然后逐步使用更复杂的调试技术。记住，好的调试习惯是成功开发的关键！
