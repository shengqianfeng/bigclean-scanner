#!/bin/bash

# BigClean Scanner Plugin 构建脚本
# 作者: Hello Sheng
# 版本: 1.0.0
# 专门使用 JDK 1.8 构建

set -e

echo "=== BigClean Scanner Plugin 构建脚本 (JDK 1.8) ==="
echo "开始构建插件..."

# 设置 JDK 1.8 环境
echo "设置 JDK 1.8 环境..."
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH=$JAVA_HOME/bin:$PATH

# 检查 Java 版本
echo "检查 Java 版本..."
java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$java_version" -ne "1" ]; then
    echo "错误: 需要 Java 1.8，当前版本: $(java -version 2>&1 | head -n 1)"
    exit 1
fi
echo "✓ Java 版本检查通过: $(java -version 2>&1 | head -n 1)"

# 检查 Gradle
echo "检查 Gradle..."
if ! command -v ./gradlew &> /dev/null; then
    echo "错误: 找不到 gradlew 脚本"
    exit 1
fi
echo "✓ Gradle 检查通过"

# 清理之前的构建
echo "跳过清理步骤（避免文件占用问题）..."
# ./gradlew clean

# 显示构建信息
echo "显示构建信息..."
echo "=== BigClean Scanner Plugin Build Info ==="
echo "Version: 1.0.0"
echo "Group: com.hello.bigclean"
echo "Java Version: 1.8"
echo "Build Directory: build"
echo "Plugin Output: build/distributions"
echo "=========================================="

# 构建插件
echo "开始构建插件..."
source ~/.bash_profile
./gradlew buildPlugin

# 检查构建结果
if [ -f "build/distributions/bigclean-scanner-1.0.0.zip" ]; then
    echo "✓ 插件构建成功!"
    echo "插件包位置: build/distributions/bigclean-scanner-1.0.0.zip"
    
    # 显示文件信息
    file_size=$(du -h "build/distributions/bigclean-scanner-1.0.0.zip" | cut -f1)
    echo "插件包大小: $file_size"
    
    # 创建发布目录
    mkdir -p release
    cp "build/distributions/bigclean-scanner-1.0.0.zip" "release/"
    echo "✓ 插件包已复制到 release/ 目录"
    
    # 显示插件包内容
    echo ""
    echo "=== 插件包内容 ==="
    unzip -l "build/distributions/bigclean-scanner-1.0.0.zip"
    
    echo ""
    echo "=== 安装说明 ==="
    echo "1. 打开 IntelliJ IDEA"
    echo "2. 进入 File → Settings → Plugins"
    echo "3. 点击齿轮图标 → Install Plugin from Disk..."
    echo "4. 选择 release/bigclean-scanner-1.0.0.zip"
    echo "5. 重启 IntelliJ IDEA"
    echo ""
    echo "=== 使用说明 ==="
    echo "1. 在右侧工具窗口找到 'BigClean Scanner'"
    echo "2. 选择要分析的项目"
    echo "3. 点击刷新按钮开始分析"
    echo "4. 查看分析结果并清理无用代码"
    
else
    echo "✗ 插件构建失败!"
    exit 1
fi

echo ""
echo "=== 构建完成 ==="
