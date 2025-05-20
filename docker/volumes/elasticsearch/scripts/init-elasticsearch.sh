#!/bin/bash

# Log functions with colors
log_info() {
  echo -e "\033[34m[INFO]\033[0m [$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

log_success() {
  echo -e "\033[32m[SUCCESS]\033[0m [$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

log_warn() {
  echo -e "\033[33m[WARN]\033[0m [$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

log_error() {
  echo -e "\033[31m[ERROR]\033[0m [$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# Configure Elasticsearch
log_info "Configuring Elasticsearch..."
cat > /usr/share/elasticsearch/config/elasticsearch.yml << EOF
ingest.geoip.downloader.enabled: false
network.host: 0.0.0.0
http.cors.enabled: true
http.cors.allow-origin: "*"
EOF

# Elasticsearch is already started in docker-compose
log_info "Elasticsearch is already started in docker-compose, waiting for service availability..."

# Check if Elasticsearch is available
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]
do
  log_info "Attempting to connect to Elasticsearch ($((RETRY_COUNT+1))/$MAX_RETRIES)"
  if curl -s "http://localhost:9200" > /dev/null; then
    log_success "Elasticsearch service is now available!"
    break
  else
    log_info "Waiting for Elasticsearch service to become available..."
    RETRY_COUNT=$((RETRY_COUNT+1))
    sleep 5
  fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
  log_warn "Warning: Elasticsearch service startup timed out, will continue with configuration attempts."
fi

# Define index prefix
INDEX_PREFIX="${ELASTICSEARCH_RUN_LOG_INDEX:-workflow_run_log_test}"
log_info "Using index prefix: ${INDEX_PREFIX}"

# Create lifecycle policy
log_info "Creating lifecycle policy..."
# Read lifecycle policy from config file
cp /scripts/config/workflow_run_log_policy.json /tmp/policy.json

# Replace variables in the template (if needed)
sed -i "s/\${INDEX_PREFIX}/${INDEX_PREFIX}/g" /tmp/policy.json

curl -s -X PUT "http://localhost:9200/_ilm/policy/${INDEX_PREFIX}_policy" -H 'Content-Type: application/json' -d @/tmp/policy.json

# Create index template
log_info "Creating index template..."

# Create temporary template file
log_info "Assembling index template..."

# Use pure bash to assemble JSON file
cat > /tmp/template.json << EOF
{
  "index_patterns": ["${INDEX_PREFIX}_*"],
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "index.lifecycle.name": "${INDEX_PREFIX}_policy",
    "index.lifecycle.rollover_alias": "${INDEX_PREFIX}"
  },
  "mappings": $(cat /scripts/config/workflow_run_log_mappings.json)
}
EOF

curl -s -X PUT "http://localhost:9200/_template/${INDEX_PREFIX}_template" -H 'Content-Type: application/json' -d @/tmp/template.json

# Create today's index
CURRENT_DATE=$(date +"%Y-%m-%d")
log_info "Creating today's index: ${INDEX_PREFIX}_${CURRENT_DATE}"

cat > /tmp/index.json << EOF
{
  "aliases": {
    "${INDEX_PREFIX}": {
      "is_write_index": true
    }
  }
}
EOF

curl -s -X PUT "http://localhost:9200/${INDEX_PREFIX}_${CURRENT_DATE}" -H 'Content-Type: application/json' -d @/tmp/index.json

# Verify index creation success
log_info "Verifying index creation..."
sleep 2
if curl -s "http://localhost:9200/${INDEX_PREFIX}_${CURRENT_DATE}" | grep -q "${INDEX_PREFIX}_${CURRENT_DATE}"; then
  log_success "Index created successfully"
else
  log_warn "Warning: Index creation may have failed"
fi

log_success "Elasticsearch initialization complete. Starting index monitoring service..."

# Start index monitoring script
chmod +x /scripts/monitor-indices.sh
/scripts/monitor-indices.sh &

# Keep script running, don't exit container
wait
