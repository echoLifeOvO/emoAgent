# 微信聊天记录分析工具

一个基于Spring Boot的微信聊天记录分析工具，提供简洁的Web界面进行智能聊天分析。

## 功能特点

- 🎯 **智能分析**：基于AI技术分析微信聊天记录
- 🌐 **Web界面**：提供现代化的Web操作界面
- 📊 **专业报告**：生成详细的分析报告
- 🚀 **一键启动**：简单的启动方式，无需复杂配置

## 快速开始

### 1. 环境要求

- Java 17 或更高版本
- Maven 3.6 或更高版本

### 2. 打包程序

```bash
mvn clean package
```

### 3. 启动程序

#### Windows用户
双击运行 `start.bat` 文件

#### Mac/Linux用户
```bash
# 给脚本执行权限
chmod +x start.sh

# 运行启动脚本
./start.sh
```

#### 方式二：命令行启动
```bash
java -jar target/emoAgent-1.0.0.jar
```

### 4. 访问界面

打开浏览器，访问：http://localhost:8080

## 使用说明

1. **输入微信数据路径**
   - 通常位于：`D:\WeChat Files\wxid_xxx\Msg`
   - 请确保文件存在且有读取权限

2. **输入好友姓名**
   - 输入要分析的好友的微信昵称或备注名

3. **开始分析**
   - 点击"开始分析"按钮
   - 等待分析完成，查看结果

## 项目结构

```
emoAgent/
├── src/main/java/com/emotest/emoAgent/
│   ├── controller/
│   │   ├── AnalyzeController.java    # API控制器
│   │   └── WebController.java        # Web页面控制器
│   ├── service/
│   │   └── analyzeChatMsg/           # 分析服务
│   └── EmoAgentApplication.java      # 主启动类
├── src/main/resources/
│   ├── templates/
│   │   └── index.html               # 前端页面
│   └── application.yml              # 配置文件
├── start.bat                        # 启动脚本
└── README.md                        # 说明文档
```

## 技术栈

- **后端**：Spring Boot 3.2.0
- **前端**：HTML5 + CSS3 + JavaScript
- **模板引擎**：Thymeleaf
- **AI服务**：阿里云通义千问API
- **数据库**：SQLite

## 注意事项

1. 确保微信数据文件路径正确
2. 确保有足够的文件读取权限
3. 分析过程可能需要一些时间，请耐心等待
4. 建议在分析前备份重要数据

## 故障排除

### 常见问题

1. **Java环境问题**
   - 确保安装了Java 17或更高版本
   - 检查JAVA_HOME环境变量设置

2. **端口占用**
   - 默认端口8080被占用时，修改application.yml中的server.port

3. **文件权限**
   - 确保对微信数据文件有读取权限
   - 以管理员身份运行程序

4. **网络问题**
   - 确保能够访问AI API服务
   - 检查防火墙设置

## 许可证

本项目仅供学习和研究使用。 