#!/bin/bash

# BigClean Scanner 插件调试脚本
# 使用方法: ./debug.sh [选项]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助信息
show_help() {
    echo "BigClean Scanner 插件调试脚本"
    echo ""
    echo "使用方法:"
    echo "  ./debug.sh [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help          显示此帮助信息"
    echo "  -r, --run           启动调试实例（普通模式）"
    echo "  -d, --debug         启动调试实例（调试模式）"
    echo "  -b, --build         构建插件"
    echo "  -c, --clean         清理构建文件"
    echo "  -l, --log           查看日志文件"
    echo "  -t, --tail          实时查看日志"
    echo "  -i, --info          显示构建信息"
    echo "  --setup-debug       设置调试环境"
    echo ""
    echo "示例:"
    echo "  ./debug.sh -d       # 启动调试模式"
    echo "  ./debug.sh -b -r    # 构建并运行"
    echo "  ./debug.sh -t       # 实时查看日志"
}

# 检查Gradle是否可用
check_gradle() {
    if [ -f "./gradlew" ]; then
        GRADLE_CMD="./gradlew"
    elif command -v gradle &> /dev/null; then
        GRADLE_CMD="gradle"
    else
        print_error "找不到Gradle，请确保项目中有gradlew或系统中安装了Gradle"
        exit 1
    fi
}

# 构建插件
build_plugin() {
    print_info "开始构建插件..."
    $GRADLE_CMD clean buildPlugin
    print_success "插件构建完成"
}

# 启动调试实例（普通模式）
run_ide() {
    print_info "启动IDEA调试实例（普通模式）..."
    print_warning "这将启动一个新的IDEA实例，请在其中测试插件功能"
    $GRADLE_CMD runIde
}

# 启动调试实例（调试模式）
debug_ide() {
    print_info "启动IDEA调试实例（调试模式）..."
    print_warning "调试端口: 5005"
    print_warning "请在开发IDEA中设置远程调试配置连接到localhost:5005"
    $GRADLE_CMD runIde --debug-jvm
}

# 清理构建文件
clean_build() {
    print_info "清理构建文件..."
    $GRADLE_CMD clean
    print_success "清理完成"
}

# 显示构建信息
show_build_info() {
    print_info "显示构建信息..."
    $GRADLE_CMD properties | grep -E "(version|group|name)"
    
    if [ -f "build.gradle" ]; then
        print_info "Gradle配置信息:"
        echo "  - IntelliJ版本: $(grep -o "version = '[^']*'" build.gradle | cut -d"'" -f2)"
        echo "  - 插件类型: $(grep -o "type = '[^']*'" build.gradle | cut -d"'" -f2)"
        echo "  - Java版本: $(grep -o "JavaVersion.VERSION_[^']*" build.gradle)"
    fi
}

# 查找日志文件
find_log_file() {
    # macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        LOG_DIR="$HOME/Library/Logs/JetBrains"
        if [ -d "$LOG_DIR" ]; then
            LOG_FILE=$(find "$LOG_DIR" -name "idea.log" -type f | head -1)
        fi
    # Linux
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        LOG_DIR="$HOME/.cache/JetBrains"
        if [ -d "$LOG_DIR" ]; then
            LOG_FILE=$(find "$LOG_DIR" -name "idea.log" -type f | head -1)
        fi
    # Windows (Git Bash)
    elif [[ "$OSTYPE" == "msys" ]]; then
        LOG_DIR="$APPDATA/JetBrains"
        if [ -d "$LOG_DIR" ]; then
            LOG_FILE=$(find "$LOG_DIR" -name "idea.log" -type f | head -1)
        fi
    fi
    
    echo "$LOG_FILE"
}

# 查看日志文件
view_log() {
    LOG_FILE=$(find_log_file)
    
    if [ -n "$LOG_FILE" ] && [ -f "$LOG_FILE" ]; then
        print_info "查看日志文件: $LOG_FILE"
        tail -100 "$LOG_FILE"
    else
        print_warning "找不到日志文件，请确保已经运行过IDEA"
        print_info "日志文件通常位于:"
        echo "  - macOS: ~/Library/Logs/JetBrains/IntelliJIdea*/idea.log"
        echo "  - Linux: ~/.cache/JetBrains/IntelliJIdea*/log/idea.log"
        echo "  - Windows: %APPDATA%\\JetBrains\\IntelliJIdea*\\log\\idea.log"
    fi
}

# 实时查看日志
tail_log() {
    LOG_FILE=$(find_log_file)
    
    if [ -n "$LOG_FILE" ] && [ -f "$LOG_FILE" ]; then
        print_info "实时查看日志文件: $LOG_FILE"
        print_info "按Ctrl+C停止查看"
        tail -f "$LOG_FILE" | grep --color=auto -E "(bigclean|BigClean|ERROR|Exception|===)"
    else
        print_warning "找不到日志文件，请先运行IDEA实例"
    fi
}

# 设置调试环境
setup_debug() {
    print_info "设置调试环境..."
    
    # 检查是否存在调试配置
    if ! grep -q "debug-jvm" build.gradle 2>/dev/null; then
        print_info "在build.gradle中添加调试配置..."
        
        # 备份原文件
        cp build.gradle build.gradle.backup
        
        # 添加调试配置
        cat >> build.gradle << 'EOF'

// 调试配置
runIde {
    jvmArgs = [
        '-Xdebug',
        '-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005',
        '-Xms512m',
        '-Xmx2048m'
    ]
    systemProperties = [
        'idea.debug.mode': 'true',
        'idea.log.debug.categories': 'com.hello.bigclean'
    ]
}
EOF
        print_success "调试配置已添加到build.gradle"
    else
        print_info "调试配置已存在"
    fi
    
    print_info "调试环境设置完成！"
    print_info "使用 './debug.sh -d' 启动调试模式"
}

# 主函数
main() {
    # 检查是否在项目根目录
    if [ ! -f "build.gradle" ]; then
        print_error "请在项目根目录运行此脚本"
        exit 1
    fi
    
    # 检查Gradle
    check_gradle
    
    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -r|--run)
                run_ide
                shift
                ;;
            -d|--debug)
                debug_ide
                shift
                ;;
            -b|--build)
                build_plugin
                shift
                ;;
            -c|--clean)
                clean_build
                shift
                ;;
            -l|--log)
                view_log
                shift
                ;;
            -t|--tail)
                tail_log
                shift
                ;;
            -i|--info)
                show_build_info
                shift
                ;;
            --setup-debug)
                setup_debug
                shift
                ;;
            *)
                print_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 如果没有参数，显示帮助
    if [ $# -eq 0 ]; then
        show_help
    fi
}

# 运行主函数
main "$@"
