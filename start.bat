@echo off
echo ========================================
echo    微信聊天记录分析工具启动程序
echo ========================================
echo.

echo 正在启动微信聊天记录分析服务...
echo.

REM 检查Java是否安装
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误：未检测到Java环境，请先安装Java 17或更高版本
    echo 下载地址：https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM 检查jar文件是否存在
if not exist "target\wechat-chat-analyzer-1.0.0.jar" (
    echo 错误：未找到可执行文件，请先运行 mvn clean package 进行打包
    pause
    exit /b 1
)

echo 启动服务中，请稍候...
echo 服务启动后，请在浏览器中访问：http://localhost:8080
echo 按 Ctrl+C 可以停止服务
echo.

java -jar target\wechat-chat-analyzer-1.0.0.jar

pause 