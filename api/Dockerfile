FROM maven:3.8.4-openjdk-8 AS builder

# 设置工作目录
WORKDIR /build

# 复制 settings.xml
RUN mkdir -p /root/.m2
COPY ../resources/settings.xml /root/.m2/

# 先复制 pom.xml 以实现依赖缓存
COPY pom.xml /build/

# 复制本地依赖包
COPY ../resources/bella-job-queue-sdk-1.0.0-SNAPSHOT.jar /build/

# 安装本地依赖 bella-job-queue-sdk 到 Maven 本地仓库
RUN mvn install:install-file \
    -Dfile=/build/bella-job-queue-sdk-1.0.0-SNAPSHOT.jar \
    -DgroupId=com.ke.bella.job.queue \
    -DartifactId=bella-job-queue-sdk \
    -Dversion=1.0.0-SNAPSHOT \
    -Dpackaging=jar

# 预下载所有依赖，利用 Docker 的层缓存机制
RUN mvn dependency:go-offline -B

# 复制其余源代码
COPY . /build/

# 设置脚本权限
RUN chmod +x /build/build.sh /build/run.sh /build/setenv.sh

# 接收构建参数
ARG MAVEN_ARGS="-Dmaven.test.skip=true -U -Dmaven.build.cache.enabled=false"

# 执行构建
RUN cd /build && bash build.sh ${MAVEN_ARGS}

# 第二阶段：运行环境
FROM openjdk:8

# 设置环境变量
ENV IDC zeus
ENV MODULE bella-workflow
ENV ENVTYPE test
ENV DEBUGPORT 9008
ENV JMXPORT 9009
ENV MATRIX_CODE_DIR /opt/bella-workflow/htdocs
ENV MATRIX_APPLOGS_DIR /opt/bella-workflow/applogs
ENV MATRIX_ACCESSLOGS_DIR /opt/bella-workflow/logs
ENV MATRIX_LOGS_DIR /opt/bella-workflow/logs
ENV MATRIX_CACHE_DIR /opt/bella-workflow/cache
ENV MATRIX_PRIVDATA_DIR /opt/bella-workflow/privdata

# 创建必要的目录
RUN mkdir -p /opt/bella-workflow/htdocs/bin /opt/bella-workflow/htdocs/lib \
    /opt/bella-workflow/applogs /opt/bella-workflow/logs \
    /opt/bella-workflow/cache /opt/bella-workflow/privdata

# 从构建阶段复制构建结果
COPY --from=builder /build/release/bin/* /opt/bella-workflow/htdocs/bin/
COPY --from=builder /build/release/lib/* /opt/bella-workflow/htdocs/lib/

# 设置可执行权限
RUN chmod +x /opt/bella-workflow/htdocs/bin/*.sh

# 暴露端口
EXPOSE 8080 9008 9009

# 设置工作目录
WORKDIR /opt/bella-workflow/htdocs

# 声明卷
VOLUME ["/opt/bella-workflow/applogs", "/opt/bella-workflow/logs", "/opt/bella-workflow/cache", "/opt/bella-workflow/privdata"]

# 启动命令
CMD ["/bin/bash", "-x", "/opt/bella-workflow/htdocs/bin/run.sh"]
