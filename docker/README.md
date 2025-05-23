# Bella Workflow 部署指南

## 前置依赖

**重要**：Bella Workflow 依赖于 [bella-openapi](https://github.com/LianjiaTech/bella-openapi) 项目，您需要：

1. 首先部署 bella-openapi 项目并确保其正常运行
2. 获取 bella-openapi 的访问地址和相关配置
3. 在部署 Bella Workflow 前，修改对应的环境变量文件中的 bella-openapi 相关配置

如果没有正确配置 bella-openapi，Bella Workflow 将无法正常工作。

## 环境变量配置说明

项目提供了两个环境变量模板文件：

- `.example.env`：完整部署环境变量配置
- `.middleware.env`：仅中间件服务的环境变量配置

### bella-openapi 相关配置

在环境变量文件中，您需要特别注意以下与 bella-openapi 相关的配置项：

```
# bella-openapi 服务地址配置
BELLA_OPENAPI_URL=http://your-bella-openapi-host:port

# 其他相关配置项
# 如认证信息、API 密钥等
```

请确保将上述配置项替换为您实际部署的 bella-openapi 服务地址和相关参数。

## 快速启动（一键部署）

建议通过复制环境变量文件并修改其中的值来启动服务：

```bash
# 在 docker 目录下执行

# 1. 复制环境变量模板文件
# 完整部署模板

cp .example.env .env

# 2. 修改环境变量文件中的配置，特别是 bella-openapi 相关的配置
# 使用你喜欢的编辑器打开并修改
# vi .env

# 3. 使用修改后的环境变量文件启动服务
docker-compose --env-file .env -f docker-compose.yaml up -d
```

> **提示**：添加 `-d` 参数可在后台运行容器，方便您继续使用终端。

访问地址：启动成功后，可通过浏览器访问 `http://localhost:3000` 进入 Bella Workflow 应用界面。

## 本地开发模式（分步启动）

### 1. 启动依赖中间件

如果您需要进行本地开发，可以只启动必要的中间件服务（如数据库、Redis等）：

```bash
# 在 docker 目录下执行

# 1. 复制中间件环境变量模板文件
cp .middleware.env .my-middleware.env

# 2. 根据需要修改环境变量文件
# vi .my-middleware.env

# 3. 使用修改后的环境变量文件启动中间件服务
docker-compose --env-file .my-middleware.env -f docker-compose.yaml up -d
```

### 2. 启动后端服务

在完成中间件启动后，您可以在本地启动后端服务：

```bash
# 在项目根目录下执行
./mvnw spring-boot:run
```

### 3. 启动前端服务

```bash
# 在 web 目录下执行
npm install
npm run dev
```

## 常用操作命令

```bash
# 查看运行中的容器
docker-compose ps

# 停止并移除所有容器
docker-compose down

# 查看容器日志
docker-compose logs -f [服务名称]

# 重建并启动特定服务
docker-compose up -d --build [服务名称]
```

## 部署顺序

为确保系统正常运行，请按照以下顺序进行部署：

1. 部署 [bella-openapi](https://github.com/LianjiaTech/bella-openapi) 并确保其正常运行
2. 修改 Bella Workflow 环境变量文件中的 bella-openapi 相关配置
3. 启动 Bella Workflow 服务

**注意**：如果跳过了第一步或配置不正确，将导致功能无法正常使用。

## 故障排除

如果您遇到部署或运行问题，请检查：

1. bella-openapi 服务是否正常运行
2. 环境变量文件中的 bella-openapi 相关配置是否正确
3. 查看容器日志了解具体错误信息：`docker-compose logs -f`

## 注意事项

- 首次启动可能需要较长时间，请耐心等待
- 确保端口未被占用（默认使用：3000、8080、5432、6379等）
- 生产环境部署前，请修改默认密码和配置