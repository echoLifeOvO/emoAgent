#!/bin/bash

echo "=== Java自动安装测试脚本 ==="
echo

# 测试Java检测功能
echo "1. 测试Java检测..."
if command -v java &> /dev/null; then
    echo "✓ Java已安装: $(java -version 2>&1 | head -n 1)"
else
    echo "✗ Java未安装"
fi

echo

# 测试操作系统检测
echo "2. 测试操作系统检测..."
echo "操作系统类型: $OSTYPE"
echo "架构: $(uname -m)"

if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "✓ 检测到macOS系统"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "✓ 检测到Linux系统"
else
    echo "✗ 未知操作系统"
fi

echo

# 测试包管理器检测
echo "3. 测试包管理器检测..."
if command -v brew &> /dev/null; then
    echo "✓ Homebrew已安装"
elif command -v apt-get &> /dev/null; then
    echo "✓ apt-get可用 (Ubuntu/Debian)"
elif command -v yum &> /dev/null; then
    echo "✓ yum可用 (CentOS/RHEL)"
elif command -v dnf &> /dev/null; then
    echo "✓ dnf可用 (Fedora)"
else
    echo "✗ 未检测到支持的包管理器"
fi

echo

# 测试权限
echo "4. 测试权限..."
if [ "$EUID" -eq 0 ]; then
    echo "✓ 以root权限运行"
else
    echo "⚠ 以普通用户权限运行（某些操作可能需要sudo）"
fi

echo

echo "=== 测试完成 ==="
echo "如果所有检测都通过，启动脚本应该能正常工作"
 