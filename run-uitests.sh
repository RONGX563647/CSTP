#!/bin/bash
# AiSale项目UI测试运行脚本

echo "==========================================="
echo "    AiSale项目UI测试套件"
echo "==========================================="

# 设置颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  run     - 运行所有UI测试"
    echo "  ui      - 在UI模式下运行测试"
    echo "  headed  - 在有头模式下运行测试（显示浏览器窗口）"
    echo "  install - 安装Playwright依赖"
    echo "  list    - 列出所有测试文件"
    echo "  help    - 显示此帮助信息"
    echo ""
}

# 检查node和npm是否已安装
check_dependencies() {
    if ! command -v node &> /dev/null; then
        echo -e "${RED}错误: node未安装${NC}"
        exit 1
    fi

    if ! command -v npm &> /dev/null; then
        echo -e "${RED}错误: npm未安装${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ 依赖检查通过${NC}"
}

# 安装Playwright
install_playwright() {
    echo -e "${BLUE}正在安装Playwright...${NC}"
    cd frontend
    npm install -D @playwright/test@latest
    npx playwright install
    cd ..
    echo -e "${GREEN}✓ Playwright安装完成${NC}"
}

# 列出测试文件
list_tests() {
    echo -e "${BLUE}找到的UI测试文件:${NC}"
    find frontend/tests/ui -name "*.spec.ts" -type f | sort
    echo ""
}

# 运行所有UI测试
run_tests() {
    echo -e "${BLUE}正在启动后台服务器...${NC}"

    # 启动开发服务器
    cd frontend
    npm run dev > /dev/null 2>&1 &
    SERVER_PID=$!

    # 等待服务器启动
    echo -e "${YELLOW}等待服务器启动...${NC}"
    sleep 10

    echo -e "${BLUE}开始运行UI测试...${NC}"

    # 运行测试
    npx playwright test

    TEST_RESULT=$?

    # 杀掉后台服务器进程
    kill $SERVER_PID 2>/dev/null

    if [ $TEST_RESULT -eq 0 ]; then
        echo -e "${GREEN}✓ 所有UI测试通过!${NC}"
    else
        echo -e "${RED}✗ 一些UI测试失败${NC}"
    fi

    cd ..
    return $TEST_RESULT
}

# 在UI模式下运行测试
run_ui_tests() {
    echo -e "${BLUE}启动UI模式测试...${NC}"
    cd frontend
    npm run dev > /dev/null 2>&1 &
    SERVER_PID=$!

    # 等待服务器启动
    sleep 10

    # 运行UI模式测试
    npx playwright test --ui

    kill $SERVER_PID 2>/dev/null
    cd ..
}

# 在有头模式下运行测试
run_headed_tests() {
    echo -e "${BLUE}启动有头模式测试...${NC}"
    cd frontend
    npm run dev > /dev/null 2>&1 &
    SERVER_PID=$!

    # 等待服务器启动
    sleep 10

    # 运行有头模式测试
    npx playwright test --headed

    kill $SERVER_PID 2>/dev/null
    cd ..
}

# 主逻辑
case "$1" in
    "run")
        check_dependencies
        list_tests
        run_tests
        ;;
    "ui")
        check_dependencies
        run_ui_tests
        ;;
    "headed")
        check_dependencies
        run_headed_tests
        ;;
    "install")
        check_dependencies
        install_playwright
        ;;
    "list")
        list_tests
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    "")
        echo -e "${YELLOW}未指定选项。使用 '$0 help' 查看可用选项。${NC}"
        show_help
        ;;
    *)
        echo -e "${RED}未知选项: $1${NC}"
        show_help
        exit 1
        ;;
esac