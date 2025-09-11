# BigClean Scanner - IntelliJ IDEA 插件

<div align="center">
  <img src="src/main/resources/icons/pluginIcon.svg" alt="BigClean Scanner Icon" width="128" height="128">
</div>

一个强大的 IntelliJ IDEA 插件，用于分析项目中的无用代码，帮助开发者清理和维护代码库。

## 🚀 主要功能

### 无用类分析
- **正则表达式分析**：快速扫描，适合初步分析
- **Spoon AST分析**：精确的语法分析，提供更准确的结果

### 无用方法分析
- 识别项目中未被调用的方法
- 显示方法行数信息

### 独立刷新功能
- 每个分析模块都可以独立刷新
- 全局刷新所有分析结果

### 框架支持
- Spring 框架：@Component, @Service, @Repository, @Controller
- AspectJ：@Aspect 注解
- MapStruct：@Mapper 注解
- Lombok：各种注解类
- JUnit：测试类
- 配置属性类

## 📦 安装方法

### 方法一：从插件包安装
1. 下载 `bigclean-scanner-1.0.0.zip` 插件包
2. 打开 IntelliJ IDEA
3. 进入 `File` → `Settings` → `Plugins`
4. 点击齿轮图标 → `Install Plugin from Disk...`
5. 选择下载的插件包文件
6. 重启 IntelliJ IDEA

### 方法二：从源码构建
```bash
# 注意：源代码不公开提供
# 如需源码访问权限，请联系作者
```

## 🛠️ 使用方法

### 1. 打开工具窗口
- 在 IntelliJ IDEA 右侧找到 "BigClean Scanner" 工具窗口
- 或者通过 `View` → `Tool Windows` → `BigClean Scanner` 打开

### 2. 选择分析类型
- **无用类** 标签页：包含正则表达式分析和 Spoon AST 分析
- **无用方法** 标签页：分析未被使用的方法

### 3. 开始分析
- 点击对应标签页中的刷新按钮开始分析
- 或者点击顶部的"刷新所有分析"按钮

### 4. 查看结果
- 分析结果以树形结构显示
- 展开节点查看详细信息
- 右键点击可以复制类名或方法名

## 🔧 开发环境要求

- **Java**: JDK 8 
- **IntelliJ IDEA**: 2023.1 或更高版本
- **Gradle**: 6.1 或更高版本

## 🏗️ 构建项目

```bash
# 显示构建信息
./gradlew printBuildInfo

# 构建插件
./gradlew buildPlugin

# 清理构建文件
./gradlew cleanBuild

# 运行测试
./gradlew test
```

## 📁 项目结构

```
src/main/java/com/hello/bigclean/
├── ClassPathAnalyzerFactory.java    # 主工厂类
├── handler/                         # 处理器包
│   ├── RegexUnusedClassAnalyzer.java
│   ├── SpoonUnusedClassAnalyzer.java
│   └── MethodModelHandler.java
└── spoon/                          # Spoon 分析包
    ├── strategy/                   # 分析策略
    ├── helper/                     # 辅助工具
    └── reference/                  # 引用模型
```

## ⚠️ 注意事项

1. **分析结果仅供参考**：请在实际删除代码前仔细确认
2. **备份重要代码**：建议在清理前备份项目
3. **测试验证**：删除代码后请运行测试确保功能正常
4. **框架特性**：某些框架可能通过反射或动态代理使用类，分析工具可能无法完全识别

## 🐛 常见问题

### Q: 分析结果为空？
A: 请检查项目路径是否正确，确保项目包含 Java 源文件。

### Q: Spoon AST 分析失败？
A: 可能是类加载器冲突，请重启 IntelliJ IDEA 或检查项目配置。

### Q: 插件无法安装？
A: 请确保 IntelliJ IDEA 版本兼容（2023.1+），并检查插件包是否完整。

## 📄 许可证

本插件为专有软件，版权所有。未经作者明确书面许可，不得以任何形式重新分发或使用源代码。

## 🤝 支持与反馈

如有问题或建议，请通过邮件联系作者。

## 📞 联系方式

- 作者：Hello Sheng
- 邮箱：1007424128@qq.com

---

**注意**：此插件仍在开发中，请谨慎使用分析结果。如有问题，请及时反馈。
