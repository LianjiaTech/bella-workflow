#!/bin/bash

# Bella Workflow 多架构Docker构建脚本，专用于GitHub Actions

set -e

REGISTRY=${1:-bellatop}
VERSION=${2:-latest}
PLATFORMS="linux/amd64,linux/arm64"

echo "构建 Bella Workflow 多架构Docker镜像..."
echo "仓库: $REGISTRY"
echo "版本: $VERSION"
echo "平台: $PLATFORMS"

# 清理旧的构建器（如果存在）
echo "清理旧的构建器..."
docker buildx rm multibuilder 2>/dev/null || true

# 设置buildx
echo "创建新的构建器..."
docker buildx create --name multibuilder --driver docker-container --bootstrap --use

# 清理构建缓存
echo "清理构建缓存..."
docker buildx prune -f

# 构建API镜像
echo "构建API镜像..."
docker buildx build \
  --platform $PLATFORMS \
  --build-arg VERSION=$VERSION \
  --build-arg REGISTRY=$REGISTRY \
  --no-cache \
  -t $REGISTRY/bella-workflow-api:$VERSION \
  -t $REGISTRY/bella-workflow-api:latest \
  -f api/Dockerfile \
  --push .

# 构建Web镜像  
echo "构建Web镜像..."
docker buildx build \
  --platform $PLATFORMS \
  --build-arg VERSION=$VERSION \
  --build-arg REGISTRY=$REGISTRY \
  --no-cache \
  -t $REGISTRY/bella-workflow-web:$VERSION \
  -t $REGISTRY/bella-workflow-web:latest \
  -f web/Dockerfile \
  --push .

# 构建Tasks镜像
echo "构建Tasks镜像..."
docker buildx build \
  --platform $PLATFORMS \
  --build-arg VERSION=$VERSION \
  --build-arg REGISTRY=$REGISTRY \
  --no-cache \
  -t $REGISTRY/bella-workflow-tasks:$VERSION \
  -t $REGISTRY/bella-workflow-tasks:latest \
  -f tasks/Dockerfile \
  --push .

# 清理构建器
echo "清理构建器..."
docker buildx rm multibuilder 2>/dev/null || true

echo "✅ 所有模块多架构镜像构建完成"