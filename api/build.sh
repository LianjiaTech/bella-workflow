#!/bin/sh
# 构建命令为 sh build.sh ARGS_DEV, 可传入ARGS_DEV作为用户变量 如 -Dmaven.test.skip=true

ARGS_DEV="$@"

set -e

# fixme: 安装本地依赖 bella-job-queue-sdk 到 Maven 本地仓库；等待job-queue开源后则可去掉
echo "Installing local dependencies..."
mvn install:install-file \
    -Dfile=bella-job-queue-sdk-1.0.1-SNAPSHOT.jar \
    -DgroupId=com.ke.bella.job.queue \
    -DartifactId=bella-job-queue-sdk \
    -Dversion=1.0.1-SNAPSHOT \
    -Dpackaging=jar

rm -rf release/
mvn clean package ${ARGS_DEV}

mkdir -p release/{bin,lib}
chmod +x setenv.sh && cp setenv.sh release/bin/
chmod +x run.sh && cp run.sh release/bin/
cp target/*.jar release/lib/
tar czvf release.tar.gz release
