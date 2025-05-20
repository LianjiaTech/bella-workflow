#!/bin/bash

# Index Monitoring Script - Periodically checks and ensures indices exist for today and tomorrow

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

# Check if Elasticsearch is available
check_es_available() {
  until curl -s "http://localhost:9200" > /dev/null; do
    log_info "Waiting for Elasticsearch service to become available..."
    sleep 5
  done
  log_success "Elasticsearch service is now available"
}

# Get current date formatted as YYYY-MM-DD
get_current_date() {
  date +"%Y-%m-%d"
}

# Get tomorrow's date formatted as YYYY-MM-DD
get_next_date() {
  date -d "tomorrow" +"%Y-%m-%d"
}

# Check if index exists
check_index_exists() {
  local index_name=$1
  local status_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:9200/${index_name}")
  
  if [[ "$status_code" == "200" ]]; then
    return 0  # Index exists
  else
    return 1  # Index does not exist
  fi
}

# Create index for specified date
create_index_for_date() {
  local date_str=$1
  local is_write_index=$2
  local index_prefix="${ELASTICSEARCH_RUN_LOG_INDEX:-workflow_run_log_test}"
  local index_name="${index_prefix}_${date_str}"
  
  log_info "Creating index: ${index_name}"
  
  # Skip if index already exists
  if check_index_exists "${index_name}"; then
    log_info "Index ${index_name} already exists, skipping creation"
    return 0
  fi
  
  # Display all current indices (before creation)
  log_info "Current indices (before creation):"
  curl -s "http://localhost:9200/_cat/indices/${index_name}*?v" || log_info "No matching indices found"
  
  # Prepare alias configuration
  local alias_config
  if [[ "$is_write_index" == "true" ]]; then
    alias_config='{"'"${index_prefix}"'":{"is_write_index":true}}'
  else
    alias_config='{"'"${index_prefix}"'":{}}'
  fi
  
  # Combine index configuration, including mappings and aliases
  cat > /tmp/index_create_${date_str}.json << EOF
{
  "mappings": $(cat /scripts/config/workflow_run_log_mappings.json),
  "aliases": ${alias_config}
}
EOF
  
  # Create index and apply mappings
  local response=$(curl -s -X PUT "http://localhost:9200/${index_name}" -H 'Content-Type: application/json' -d @/tmp/index_create_${date_str}.json)
  
  if echo "$response" | grep -q '"acknowledged":true'; then
    log_success "Index ${index_name} created successfully"
    
    # Verify index creation success and mapping application
    log_info "Verifying index mapping application..."
    sleep 2
    local mapping_check=$(curl -s "http://localhost:9200/${index_name}/_mapping")
    if echo "$mapping_check" | grep -q "properties"; then
      log_success "Mapping successfully applied to index ${index_name}"
    else
      log_warn "Warning: Mapping may not have been correctly applied to index ${index_name}"
    fi
    
    # Display all current indices (after creation)
    log_info "Current indices (after creation):"
    curl -s "http://localhost:9200/_cat/indices/${index_name}*?v"
    
    return 0
  else
    log_error "Index ${index_name} creation failed: $response"
    return 1
  fi
}

# Check and ensure indices exist
check_and_ensure_indices() {
  log_info "Starting index status check..."
  
  # Get current date and tomorrow's date
  local current_date=$(get_current_date)
  local next_date=$(get_next_date)
  
  log_info "Current date: ${current_date}, tomorrow's date: ${next_date}"
  
  # Create today's index (if it doesn't exist)
  create_index_for_date "${current_date}" "true"
  
  # Create tomorrow's index (if it doesn't exist)
  create_index_for_date "${next_date}" "false"
  
  log_success "Index check completed"
}

# Clean up expired indices
cleanup_old_indices() {
  log_info "Checking and cleaning up expired indices..."
  
  # Calculate target date (7 days ago)
  local target_date=$(date -d "7 days ago" +"%Y-%m-%d")
  
  # Get all indices
  local index_prefix="${ELASTICSEARCH_RUN_LOG_INDEX:-workflow_run_log_test}"
  local indices=$(curl -s "http://localhost:9200/_cat/indices/${index_prefix}_*?h=index" | sort)
  
  for index in $indices; do
    # Extract date from index name
    local index_date=$(echo $index | sed -E "s/${index_prefix}_([0-9]{4}-[0-9]{2}-[0-9]{2})/\1/")
    
    # Delete index if date is earlier than target date
    if [[ "$index_date" < "$target_date" ]]; then
      log_info "Deleting expired index: $index (earlier than $target_date)"
      curl -s -X DELETE "http://localhost:9200/$index" > /dev/null
    fi
  done
  
  log_success "Expired indices cleanup completed"
}

# Main function
main() {
  log_info "Index monitoring service started"
  
  # First check if Elasticsearch is available
  check_es_available
  
  # Initial check
  check_and_ensure_indices
  
  # Clean up expired indices
  cleanup_old_indices
  
  # Check every hour
  while true; do
    log_info "Waiting for next check cycle..."
    sleep 3600  # Check once every hour
    check_and_ensure_indices
    cleanup_old_indices
  done
}

# Start main function
main
