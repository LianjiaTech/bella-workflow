FROM maven:3.8.4-openjdk-8 AS build

# 设置工作目录
WORKDIR /build

# 首先只复制pom.xml文件
COPY pom.xml .

# 创建空的src目录结构以确保Maven可以正确解析
RUN mkdir -p src/main/java src/main/resources

# 预先下载所有依赖项（利用Docker缓存机制）
# 这一步只有当pom.xml发生变化时才会重新执行
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src/ ./src/

# 打包应用
RUN mvn clean package -DskipTests

# 第二阶段：运行环境
FROM flink:1.18.1-scala_2.12-java8

# 设置工作目录
WORKDIR /opt/flink

# 复制默认配置文件
COPY --from=build /build/src/main/resources/application.properties /opt/flink/conf/

# 复制打包好的JAR文件
COPY --from=build /build/target/tasks-1.0-SNAPSHOT.jar /opt/flink/usrlib/tasks.jar

# 创建日志目录
RUN mkdir -p /opt/flink/logs

# 创建启动脚本
COPY docker-entrypoint.sh /opt/flink/docker-entrypoint.sh
RUN chmod +x /opt/flink/docker-entrypoint.sh

# 暴露Flink Web UI端口
EXPOSE 8081

# 设置环境变量（可在运行时覆盖）
# 默认为 local 模式，使用本地环境运行
ENV EXECUTION_MODE="local"
# 通用配置
ENV FLINK_JOB_CLASS="com.ke.bella.workflow.tasks.WorkflowRunLogKafkaToEs"

# 使用启动脚本作为入口点
ENTRYPOINT ["/opt/flink/docker-entrypoint.sh"]
