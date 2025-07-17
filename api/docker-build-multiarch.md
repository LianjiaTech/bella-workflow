# Docker Multi-Architecture Build Script

构建支持多架构的 Docker 镜像脚本，支持 AMD64 和 ARM64 架构。

## 使用方法

### 基本用法

```bash
# 本地构建多架构镜像
./docker-build-multiarch.sh

# 构建并推送到 Docker Hub
./docker-build-multiarch.sh --push

# 构建单一架构并加载到本地
./docker-build-multiarch.sh --platforms linux/amd64
```

### 命令行参数

| 参数 | 描述 | 默认值 |
|------|------|--------|
| `-n, --name NAME` | 镜像名称 | `bella-workflow-api` |
| `-t, --tag TAG` | 镜像标签 | `latest` |
| `--push` | 推送到 Docker Hub | false |
| `--platforms PLATFORMS` | 目标平台 | `linux/amd64,linux/arm64` |

### 推送到 Docker Hub

使用 `-n` 参数指定 Docker Hub 仓库：

```bash
# 推送到个人仓库
./docker-build-multiarch.sh -n username/bella-workflow-api --push

# 推送到组织仓库
./docker-build-multiarch.sh -n myorg/bella-workflow-api --push

# 指定标签推送
./docker-build-multiarch.sh -n username/bella-workflow-api -t v1.0.0 --push
```

## 前置要求

- Docker 支持 buildx 功能
- 推送前需要登录 Docker Hub：`docker login`

## 注意事项

- 多架构构建无法直接加载到本地，需要使用 `--push` 推送
- 脚本不处理认证信息，需要提前使用 `docker login` 登录
- 推送需要对应仓库的写入权限
