# Bella Workflow 部署指南

## 前置依赖

**重要**：Bella Workflow 依赖于 [bella-openapi](https://github.com/LianjiaTech/bella-openapi) 项目，您需要：

1. 首先部署 bella-openapi 项目并确保其正常运行
2. 获取 bella-openapi 的访问地址和相关配置
3. 在部署 Bella Workflow 前，修改对应的环境变量文件中的 bella-openapi 相关配置

如果没有正确配置 bella-openapi，Bella Workflow 将无法正常工作。

## 快速体验

**前置准备**：部署 [bella-openapi](https://github.com/LianjiaTech/bella-openapi) 服务并确保正常运行。

```bash
# 在 docker 目录下执行

# 1. 复制环境变量模板文件
cp .example.env .env

# 2. 修改关键配置
vi .env
# 必须修改以下两项：
# BELLA_OPENAPI_HOST=http://your-bella-openapi-host:port
# BELLA_OPENAPI_URL=http://your-bella-openapi-host:port

# 3. 启动所有服务
docker-compose --env-file .env up -d
```

启动成功后，通过浏览器访问：`http://localhost`

包含服务：Web前端、API后端、数据库、缓存、对象存储、代码沙箱、反向代理

## 本地源码部署

**适合前后端开发人员**，需要本地调试代码：

**前置准备**：部署 [bella-openapi](https://github.com/LianjiaTech/bella-openapi) 服务并确保正常运行。

```bash
# 在 docker 目录下执行

# 1. 复制中间件环境变量模板
cp .middleware.env .env

# 2. 修改bella-openapi配置
vi .env
# 必须修改：
# BELLA_OPENAPI_HOST=http://your-bella-openapi-host:port
# BELLA_OPENAPI_URL=http://your-bella-openapi-host:port

# 3. 启动中间件服务（MySQL、Redis、MinIO等）
docker-compose --env-file .env -f docker-compose.middleware.yaml up -d

# 4. 本地启动后端服务（项目根目录）
./mvnw spring-boot:run

# 5. 本地启动前端服务（web目录）
cd ../web
npm install && npm run dev
```

通过 `http://localhost` 访问完整应用。

**开发优势**：中间件容器化、应用本地化便于调试、nginx代理统一入口、热重载支持

## 企业级中间件部署

**适合生产环境**，具备高性能、大数据量处理能力：

**前置准备**：部署 [bella-openapi](https://github.com/LianjiaTech/bella-openapi) 服务并确保正常运行。

```bash
# 在 docker 目录下执行

# 1. 复制企业级环境变量模板
cp .example.es.env .env

# 2. 修改配置文件
vi .env

# 3. 启动企业级架构
docker-compose --env-file .env -f docker-compose.es.yaml up -d
```

**企业级特性**：Kafka消息队列、Flink流处理引擎、Elasticsearch搜索引擎、高可用架构

---

## SSL/HTTPS 配置（可选）

如需启用HTTPS，可在环境变量文件中配置：

```bash
# SSL/HTTPS 配置
ENABLE_SSL=true
SSL_CERT_PATH=/etc/ssl/certs/workflow/fullchain.cer
SSL_KEY_PATH=/etc/ssl/certs/workflow/private.key
SERVER_PROTOCOL=https
```

启用方法：
1. 准备SSL证书文件
2. 取消注释docker-compose文件中的证书挂载配置
3. 修改上述环境变量
4. 重启服务

---

## 常用操作

### 基本命令
```bash
# 查看运行状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 重启服务
docker-compose restart
```

### 故障排除

**常见问题检查**：
1. bella-openapi 服务是否正常运行
2. 环境变量中的 bella-openapi 配置是否正确  
3. 端口是否被占用（80、8080、3000、3306、6379等）
4. 查看详细日志：`docker-compose logs -f [服务名]`

### 端口说明

| 服务 | 端口 | 说明 |
|------|------|------|
| Web入口 | 80, 443 | 浏览器访问地址 |
| API服务 | 8080 | 后端接口服务 |
| Web前端 | 3000 | 前端开发服务器 |
| 数据库 | 3306 | MySQL数据库 |
| 缓存 | 6379 | Redis缓存 |
| 对象存储 | 9000, 9001 | MinIO文件存储 |

### 注意事项

- 首次启动需要下载镜像，请耐心等待
- 确保所需端口未被占用
- 生产环境请修改默认密码
- 如需HTTPS，准备好SSL证书文件

---

## 多租户配置

Bella Workflow 支持多租户架构。使用前需要设置默认租户的 OpenAPI 密钥：

```bash
# 在项目根目录执行（从 bella-openapi 获取密钥后）
# cd ../
./set-tenant-apikey.sh your_openapi_key_here
```