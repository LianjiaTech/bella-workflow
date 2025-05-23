# Bella Workflow 贡献指南

感谢您对 Bella Workflow 项目的关注！我们非常欢迎来自社区的贡献，无论是功能改进、文档完善还是错误修复。本文档将帮助您了解如何为 Bella Workflow 做出贡献。

## 贡献协议

在贡献代码前，请确保您同意以下条款：

1. 项目维护者有权根据项目发展需要调整开源协议
2. 您贡献的代码可能被用于商业目的
3. 您的贡献需要遵循本文档中的代码规范和流程

## 贡献流程

### 1. 准备工作

1. Fork 本仓库到您的 GitHub 账户
2. 将您的 Fork 克隆到本地：
   ```bash
   git clone https://github.com/YOUR_USERNAME/bella-workflow.git
   cd bella-workflow
   ```
3. 添加原始仓库作为远程仓库：
   ```bash
   git remote add upstream https://github.com/LianjiaTech/bella-workflow.git
   ```
4. 确保您的本地仓库与原始仓库同步：
   ```bash
   git fetch upstream
   git checkout main
   git merge upstream/main
   ```
5. 安装项目依赖的本地 JAR 包：
   
   **重要**：在本地源码启动前，您需要将 `api/resources/bella-job-queue-sdk-1.0.0-SNAPSHOT.jar` 添加到您的本地 Maven 仓库中：
   
   ```bash
   # 在项目根目录下执行
   mvn install:install-file \
     -Dfile=api/resources/bella-job-queue-sdk-1.0.0-SNAPSHOT.jar \
     -DgroupId=com.ke.bella \
     -DartifactId=bella-job-queue-sdk \
     -Dversion=1.0.0-SNAPSHOT \
     -Dpackaging=jar
   ```
   
   执行成功后，您将看到类似于“`BUILD SUCCESS`”的输出，表示 JAR 包已成功安装到您的本地 Maven 仓库中。

### 2. 创建分支

为您的贡献创建一个新分支：

```bash
# 对于功能改进
git checkout -b feature/your-feature-name

# 对于错误修复
git checkout -b fix/issue-description

# 对于文档更新
git checkout -b docs/update-description
```

### 3. 开发和测试

1. 在您的分支上进行开发工作
2. 遵循代码规范（见下文）
3. 添加或更新测试，确保所有测试通过
4. 更新相关文档（如需要）

### 4. 提交更改

1. 提交您的更改：
   ```bash
   git add .
   git commit -m "feat: add some amazing feature"
   ```
   注意：我们使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范进行提交消息格式化

2. 推送到您的 Fork：
   ```bash
   git push origin feature/your-feature-name
   ```

### 5. 创建 Pull Request

1. 在 GitHub 上前往您的 Fork
2. 点击 "Compare & pull request" 按钮
3. 提供清晰的 PR 标题和描述，解释您的更改内容和原因
4. 如果您的 PR 解决了某个 Issue，请在描述中引用该 Issue（例如："Fixes #123"）

## 代码规范

### Java 代码规范

**重要**：后端 Java 代码必须遵循项目中 `api/configuration` 目录下的 Eclipse 格式化规范：

- 使用 `eclipse-formatter.xml` 进行代码格式化
- 遵循 `eclipse.importorder` 定义的导入顺序（java > javax > org > com）
- 在 IDE 中配置这些文件以确保代码格式一致性

## 问题和讨论

- 如果您发现了错误或有功能建议，请创建 Issue
- 在开始大型工作前，最好先创建 Issue 进行讨论，以确保您的方向与项目目标一致

## 其他贡献方式

除了代码贡献外，您还可以通过以下方式支持项目：

- 改进文档
- 回答社区问题
- 分享您使用 Bella Workflow 的经验
- 在社交媒体上分享项目

## 联系我们

如果您有任何问题或需要帮助，请通过 GitHub Issues 或访问我们的[官方网站](https://doc.bella.top/)联系我们。

---

再次感谢您对 Bella Workflow 的贡献！