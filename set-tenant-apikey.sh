#!/bin/bash

# Bella Workflow 租户API密钥配置脚本
# 用于为MySQL数据库中的test租户设置OpenAPI密钥

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认配置（可通过环境变量覆盖）
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:-bella_workflow}"
MYSQL_USER="${MYSQL_USER:-bella_workflow}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-bella123}"

# 显示欢迎信息
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}    Bella Workflow 租户API密钥配置${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 检查MySQL客户端是否可用
MYSQL_CMD="mysql"
DOCKER_MYSQL=""

if ! command -v mysql &> /dev/null; then
    # 检查是否有Docker MySQL容器运行
    if command -v docker &> /dev/null && docker ps --format "table {{.Names}}" | grep -q "bella-workflow-mysql"; then
        echo -e "${YELLOW}本地未找到MySQL客户端，使用Docker容器执行${NC}"
        DOCKER_MYSQL="docker exec -i bella-workflow-mysql"
        MYSQL_CMD="$DOCKER_MYSQL mysql"
    else
        echo -e "${RED}错误: 未找到MySQL客户端且Docker容器未运行。${NC}"
        echo -e "${YELLOW}请确保已安装MySQL客户端或Docker容器正在运行。${NC}"
        echo ""
        echo -e "${BLUE}如果使用Docker部署，请先启动容器：${NC}"
        echo -e "${GREEN}docker-compose up -d mysql${NC}"
        exit 1
    fi
fi

# 显示当前配置
echo -e "${YELLOW}当前MySQL连接配置：${NC}"
echo -e "  主机: ${MYSQL_HOST}"
echo -e "  端口: ${MYSQL_PORT}"
echo -e "  数据库: ${MYSQL_DATABASE}"
echo -e "  用户: ${MYSQL_USER}"
echo ""

# 测试数据库连接
echo -e "${BLUE}正在测试数据库连接...${NC}"
if ! $MYSQL_CMD -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -e "SELECT 1;" "${MYSQL_DATABASE}" &>/dev/null; then
    echo -e "${RED}错误: 无法连接到MySQL数据库。${NC}"
    echo -e "${YELLOW}请检查：${NC}"
    echo -e "  1. MySQL服务是否正在运行"
    echo -e "  2. 连接参数是否正确"
    echo -e "  3. 用户权限是否足够"
    echo ""
    echo -e "${BLUE}如果使用Docker部署，确保容器正在运行：${NC}"
    echo -e "${GREEN}docker ps | grep mysql${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 数据库连接成功${NC}"
echo ""

# 显示当前API密钥（如果存在）
current_key=$($MYSQL_CMD -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -s -N -e "SELECT openapi_key FROM tenant WHERE tenant_id = 'test';" "${MYSQL_DATABASE}" 2>/dev/null || echo "")

if [ -n "$current_key" ] && [ "$current_key" != "NULL" ]; then
    echo -e "${YELLOW}当前API密钥: ${current_key}${NC}"
else
    echo -e "${YELLOW}当前没有设置API密钥${NC}"
fi
echo ""

# 获取API密钥（优先使用命令行参数）
if [ -n "$1" ]; then
    api_key="$1"
    echo -e "${BLUE}使用命令行参数提供的API密钥${NC}"
else
    echo -e "${BLUE}请输入新的API密钥: ${NC}"
    read -r api_key
fi

# 执行更新操作
echo ""
echo -e "${BLUE}正在更新API密钥...${NC}"

# 构建并执行SQL命令
sql_command="UPDATE tenant SET openapi_key = '${api_key}' WHERE tenant_id = 'test';"

if $MYSQL_CMD -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -e "${sql_command}" "${MYSQL_DATABASE}"; then
    echo -e "${GREEN}✓ API密钥设置成功！${NC}"
    
    # 验证更新结果
    affected_rows=$($MYSQL_CMD -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -s -N -e "SELECT ROW_COUNT();" "${MYSQL_DATABASE}")
    
    if [ "$affected_rows" -gt 0 ]; then
        echo -e "${GREEN}✓ 成功更新了 ${affected_rows} 条记录${NC}"
        
        # 最终验证：重新查询确认密钥已设置
        updated_key=$($MYSQL_CMD -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -s -N -e "SELECT openapi_key FROM tenant WHERE tenant_id = 'test';" "${MYSQL_DATABASE}" 2>/dev/null)
        if [ -n "$updated_key" ] && [ "$updated_key" = "$api_key" ]; then
            echo -e "${GREEN}✓ 验证成功：API密钥已正确设置${NC}"
        else
            echo -e "${YELLOW}⚠ 警告：设置后验证发现密钥不匹配${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ 没有记录被更新，请检查租户ID是否正确${NC}"
    fi
    
else
    echo -e "${RED}✗ API密钥设置失败${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}           操作完成！${NC}"
echo -e "${GREEN}========================================${NC}"