#!/bin/bash

# Set log directory
LOG_DIR="/workflow-logs"
CONNECT_LOG_DIR="/tmp/kafka-connect-logs"

# Ensure directory exists
mkdir -p $CONNECT_LOG_DIR
chmod 777 $CONNECT_LOG_DIR

# Wait for Kafka to fully start
echo "Waiting for Kafka to fully start..."
sleep 30

# Check if log file exists
if [ ! -f "$LOG_DIR/workflow-run.log" ]; then
  echo "Creating log file $LOG_DIR/workflow-run.log"
  touch $LOG_DIR/workflow-run.log
  chmod 666 $LOG_DIR/workflow-run.log
fi

# Create topics required by Kafka Connect
echo "Creating topics required by Kafka Connect..."
/opt/bitnami/kafka/bin/kafka-topics.sh --create --if-not-exists --bootstrap-server ${KAFKA_HOST:-kafka}:9092 --replication-factor 1 --partitions 1 --topic connect-configs
/opt/bitnami/kafka/bin/kafka-topics.sh --create --if-not-exists --bootstrap-server ${KAFKA_HOST:-kafka}:9092 --replication-factor 1 --partitions 1 --topic connect-offsets
/opt/bitnami/kafka/bin/kafka-topics.sh --create --if-not-exists --bootstrap-server ${KAFKA_HOST:-kafka}:9092 --replication-factor 1 --partitions 1 --topic connect-status
/opt/bitnami/kafka/bin/kafka-topics.sh --create --if-not-exists --bootstrap-server ${KAFKA_HOST:-kafka}:9092 --replication-factor 1 --partitions 1 --topic ${WORKFLOW_RUN_LOG_TOPIC:-workflow_run_log}

# Set up Kafka Connect configuration
echo "Configuring Kafka Connect..."
mkdir -p /opt/bitnami/kafka/config

# Ensure standard plugins are available
echo "Checking standard plugins path..."
STANDARD_PLUGINS_PATH="/opt/bitnami/kafka/libs"
echo "Standard plugins path: $STANDARD_PLUGINS_PATH"
ls -la $STANDARD_PLUGINS_PATH | grep -i connect

cat > /opt/bitnami/kafka/config/connect-standalone.properties << EOF
bootstrap.servers=${KAFKA_HOST:-kafka}:9092
key.converter=org.apache.kafka.connect.storage.StringConverter
value.converter=org.apache.kafka.connect.storage.StringConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false
offset.storage.file.filename=$CONNECT_LOG_DIR/connect.offsets
offset.flush.interval.ms=10000
plugin.path=$STANDARD_PLUGINS_PATH
EOF

# Set up file source connector configuration
cat > /opt/bitnami/kafka/config/connect-file-source.properties << EOF
name=workflow-log-connector
connector.class=org.apache.kafka.connect.file.FileStreamSourceConnector
tasks.max=1
file=$LOG_DIR/workflow-run.log
topic=${WORKFLOW_RUN_LOG_TOPIC:-workflow_run_log}
key.converter=org.apache.kafka.connect.storage.StringConverter
value.converter=org.apache.kafka.connect.storage.StringConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false
EOF

echo "Starting Kafka Connect..."
# Start connect-standalone with the correct path
nohup /opt/bitnami/kafka/bin/connect-standalone.sh /opt/bitnami/kafka/config/connect-standalone.properties /opt/bitnami/kafka/config/connect-file-source.properties > $CONNECT_LOG_DIR/kafka-connect.log 2>&1 &

echo "Kafka Connect setup completed."
echo "Log files will be collected from $LOG_DIR/workflow-run.log to topic ${WORKFLOW_RUN_LOG_TOPIC:-workflow_run_log}"

# Monitor Kafka Connect logs
tail -f $CONNECT_LOG_DIR/kafka-connect.log
