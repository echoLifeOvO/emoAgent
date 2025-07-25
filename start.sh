#!/bin/bash

# 设置错误时退出
set -e

echo "========================================"
echo "   微信聊天记录分析工具启动程序"
echo "========================================"
echo "支持的操作系统：macOS, Linux"
echo "需要的环境：Java 17+, Maven (可选)"
echo "========================================"
echo

echo "正在启动微信聊天记录分析服务..."
echo

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    echo "未检测到Java环境，正在尝试自动安装Java 17..."
    echo
    
    # 检测操作系统类型
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS系统
        echo "检测到macOS系统，使用Homebrew安装Java 17..."
        
        # 检查是否安装了Homebrew
        if ! command -v brew &> /dev/null; then
            echo "未检测到Homebrew，正在安装Homebrew..."
            /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
            
            # 添加Homebrew到PATH（针对Apple Silicon Mac）
            if [[ $(uname -m) == "arm64" ]]; then
                echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
                eval "$(/opt/homebrew/bin/brew shellenv)"
            fi
        fi
        
        echo "正在安装OpenJDK 17..."
        brew install openjdk@17
        
        # 创建符号链接
        echo "正在配置Java环境..."
        sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
        
        # 添加到PATH
        echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
        export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
        
        echo "Java 17安装完成！"
        
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux系统
        echo "检测到Linux系统，尝试使用包管理器安装Java 17..."
        
        # 检测Linux发行版
        if command -v apt-get &> /dev/null; then
            # Ubuntu/Debian
            echo "使用apt安装OpenJDK 17..."
            sudo apt-get update
            sudo apt-get install -y openjdk-17-jdk
            
        elif command -v yum &> /dev/null; then
            # CentOS/RHEL
            echo "使用yum安装OpenJDK 17..."
            sudo yum install -y java-17-openjdk-devel
            
        elif command -v dnf &> /dev/null; then
            # Fedora
            echo "使用dnf安装OpenJDK 17..."
            sudo dnf install -y java-17-openjdk-devel
            
        else
            echo "错误：无法自动安装Java，请手动安装Java 17"
            echo "下载地址：https://www.oracle.com/java/technologies/downloads/"
            exit 1
        fi
        
        echo "Java 17安装完成！"
        
    else
        echo "错误：不支持的操作系统，请手动安装Java 17"
        echo "下载地址：https://www.oracle.com/java/technologies/downloads/"
        exit 1
    fi
    
    echo "正在验证Java安装..."
    if ! command -v java &> /dev/null; then
        echo "错误：Java安装失败，请手动安装"
        exit 1
    fi
fi

# 检查Java版本
java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$java_version" -lt 17 ]; then
    echo "错误：Java版本过低，需要Java 17或更高版本"
    echo "当前版本：$(java -version 2>&1 | head -n 1)"
    exit 1
fi

# 检查jar文件是否存在
if [ ! -f "target/wechat-chat-analyzer-1.0.0.jar" ]; then
    echo "未找到可执行文件，正在自动打包..."
    echo
    
    # 检查Maven是否安装
    if ! command -v mvn &> /dev/null; then
        echo "错误：未检测到Maven，请先安装Maven"
        echo "macOS安装命令：brew install maven"
        echo "Linux安装命令：sudo apt-get install maven (Ubuntu/Debian)"
        exit 1
    fi
    
    echo "正在使用Maven打包项目..."
    mvn clean package
    
    if [ $? -ne 0 ]; then
        echo "错误：Maven打包失败，请检查项目配置"
        exit 1
    fi
    
    echo "打包完成！"
    echo
fi

# 检查端口8080是否被占用
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "警告：端口8080已被占用，正在尝试停止占用进程..."
    lsof -ti:8080 | xargs kill -9 2>/dev/null
    sleep 2
fi

echo "启动服务中，请稍候..."
echo "服务启动后，请在浏览器中访问：http://localhost:8080"
echo "按 Ctrl+C 可以停止服务"
echo

# 启动服务
echo "正在启动微信聊天记录分析服务..."
java -jar target/wechat-chat-analyzer-1.0.0.jar

echo "========================================"
echo "服务已停止"
echo "========================================"
