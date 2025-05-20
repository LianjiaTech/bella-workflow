#!/bin/bash
set -e

# 日志函数
log() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log "Starting Flink application in $EXECUTION_MODE mode"

# 检查执行模式
if [ "$EXECUTION_MODE" = "local" ]; then
  log "Running in LocalEnvironment mode"
  
  # 设置本地环境变量
  export FLINK_ENV_JAVA_OPTS="-Dexecution.runtime-mode=STREAMING"
  
  # 使用LocalEnvironment运行作业
  log "Executing job with class: $FLINK_JOB_CLASS"
  exec java -cp "/opt/flink/lib/*:/opt/flink/usrlib/*:/opt/flink/conf" \
    $FLINK_ENV_JAVA_OPTS \
    $FLINK_JOB_CLASS
  
elif [ "$EXECUTION_MODE" = "jobmanager" ]; then
  log "Running in Flink JobManager mode"
  
  # 检查是否设置了JobManager地址
  if [ -z "$FLINK_PROPERTIES" ]; then
    log "WARNING: FLINK_PROPERTIES is not set. Using default configuration."
  else
    log "Using Flink properties: $FLINK_PROPERTIES"
  fi
  
  # 使用Flink CLI提交作业到JobManager
  log "Submitting job to JobManager with class: $FLINK_JOB_CLASS"
  exec /opt/flink/bin/flink run \
    -c "$FLINK_JOB_CLASS" \
    /opt/flink/usrlib/tasks.jar \
    "$@"
    
else
  log "ERROR: Unknown execution mode: $EXECUTION_MODE"
  log "Please set EXECUTION_MODE to either 'local' or 'jobmanager'"
  exit 1
fi
