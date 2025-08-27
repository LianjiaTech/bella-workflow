#!/bin/bash
set -e

# 设置默认值
ENABLE_SSL=${ENABLE_SSL:-false}
SSL_CERT_PATH=${SSL_CERT_PATH:-/etc/ssl/certs/workflow/fullchain.cer}
SSL_KEY_PATH=${SSL_KEY_PATH:-/etc/ssl/certs/workflow/workflow.bella.top.key}
API_UPSTREAM=${API_UPSTREAM:-http://bella-workflow-api:8080}
WEB_UPSTREAM=${WEB_UPSTREAM:-http://bella-workflow-web:3000}

echo "=== Nginx Configuration ==="
echo "ENABLE_SSL: $ENABLE_SSL"
echo "SSL_CERT_PATH: $SSL_CERT_PATH"
echo "SSL_KEY_PATH: $SSL_KEY_PATH"
echo "API_UPSTREAM: $API_UPSTREAM"
echo "WEB_UPSTREAM: $WEB_UPSTREAM"

if [ "$ENABLE_SSL" = "true" ]; then
    echo "Configuring HTTPS mode..."
    
    # 检查证书文件是否存在
    if [ -f "$SSL_CERT_PATH" ] && [ -f "$SSL_KEY_PATH" ]; then
        echo "SSL certificates found, enabling HTTPS..."
        
        # SSL监听指令
        export SSL_LISTEN_DIRECTIVE="listen       443 ssl;
    listen  [::]:443 ssl;"
        
        # SSL配置块
        export SSL_CONFIG_BLOCK="http2 on;
    ssl_certificate $SSL_CERT_PATH;
    ssl_certificate_key $SSL_KEY_PATH;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;"
        
        # HTTP重定向到HTTPS
        export SSL_REDIRECT_BLOCK="# HTTP重定向到HTTPS
    if (\$scheme != \"https\") {
        return 301 https://\$host\$request_uri;
    }"
        
        # 代理转发协议
        export PROXY_FORWARDED_PROTO="https"
        
    else
        echo "Warning: SSL enabled but certificates not found!"
        echo "Certificate: $SSL_CERT_PATH (exists: $([ -f "$SSL_CERT_PATH" ] && echo 'yes' || echo 'no'))"
        echo "Private key: $SSL_KEY_PATH (exists: $([ -f "$SSL_KEY_PATH" ] && echo 'yes' || echo 'no'))"
        echo "Falling back to HTTP mode..."
        
        # 回退到HTTP模式
        export SSL_LISTEN_DIRECTIVE=""
        export SSL_CONFIG_BLOCK=""
        export SSL_REDIRECT_BLOCK=""
        export PROXY_FORWARDED_PROTO="http"
    fi
else
    echo "Configuring HTTP mode..."
    
    # HTTP模式配置
    export SSL_LISTEN_DIRECTIVE=""
    export SSL_CONFIG_BLOCK=""
    export SSL_REDIRECT_BLOCK=""
    export PROXY_FORWARDED_PROTO="http"
fi

echo "Configuration completed."
echo "================================"

# 处理nginx配置模板
if [ -f /etc/nginx/conf.d/default.conf.template ]; then
    echo "Processing nginx configuration template..."
    envsubst '${SERVER_DOMAIN} ${SSL_LISTEN_DIRECTIVE} ${SSL_CONFIG_BLOCK} ${SSL_REDIRECT_BLOCK} ${PROXY_FORWARDED_PROTO} ${API_UPSTREAM} ${WEB_UPSTREAM}' \
        < /etc/nginx/conf.d/default.conf.template \
        > /etc/nginx/conf.d/default.conf
    
    echo "Generated nginx configuration:"
    echo "--- /etc/nginx/conf.d/default.conf ---"
    cat /etc/nginx/conf.d/default.conf
    echo "--- End of configuration ---"
else
    echo "Warning: Template file not found at /etc/nginx/conf.d/default.conf.template"
fi

# 测试nginx配置
echo "Testing nginx configuration..."
nginx -t

# 启动nginx
echo "Starting nginx..."
exec nginx -g "daemon off;"