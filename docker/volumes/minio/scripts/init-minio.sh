#!/bin/bash

#==========================================================
# MinIO S3 Service Initialization Script
# This script initializes the MinIO S3 service with the
# specified bucket and root path configuration.
#==========================================================

# Log function with timestamp and color formatting
log() {
  local level=$1
  local message=$2
  local timestamp=$(date +'%Y-%m-%d %H:%M:%S')
  
  case $level in
    "INFO")
      # Blue color for info
      echo -e "\033[34m[${timestamp}] [INFO] $message\033[0m"
      ;;
    "SUCCESS")
      # Green color for success
      echo -e "\033[32m[${timestamp}] [SUCCESS] $message\033[0m"
      ;;
    "WARN")
      # Yellow color for warnings
      echo -e "\033[33m[${timestamp}] [WARN] $message\033[0m"
      ;;
    "ERROR")
      # Red color for errors
      echo -e "\033[31m[${timestamp}] [ERROR] $message\033[0m"
      ;;
    *)
      # Default color
      echo -e "[${timestamp}] $message"
      ;;
  esac
}

# Print script header
log "INFO" "Starting MinIO S3 service initialization..."
log "INFO" "Configuration:"
log "INFO" "  - Bucket: ${MINIO_BUCKET:-bella-workflow}"
log "INFO" "  - Root Path: ${MINIO_ROOT_PATH:-test-states}"

# Wait for MinIO service to be available
log "INFO" "Waiting for MinIO service to be available..."
retry_count=0
max_retries=30

# Function to check if MinIO is ready
check_minio_ready() {
  # Try to use mc directly to check if MinIO is ready
  mc alias set myminio-check http://localhost:9000 ${MINIO_ROOT_USER:-minioadmin} ${MINIO_ROOT_PASSWORD:-minioadmin} > /dev/null 2>&1
  return $?
}

# Keep trying until MinIO is ready
while ! check_minio_ready; do
  retry_count=$((retry_count+1))
  if [ $retry_count -ge $max_retries ]; then
    log "ERROR" "MinIO service failed to start after ${max_retries} attempts. Exiting."
    exit 1
  fi
  log "INFO" "Waiting for MinIO service to be available... (Attempt ${retry_count}/${max_retries})"
  sleep 2
done

log "SUCCESS" "MinIO service is now available. Starting configuration..."

# Configure MinIO client (mc)
log "INFO" "Configuring MinIO client..."
if mc alias set myminio http://localhost:9000 ${MINIO_ROOT_USER:-minioadmin} ${MINIO_ROOT_PASSWORD:-minioadmin} > /dev/null 2>&1; then
  log "SUCCESS" "MinIO client configured successfully"
else
  log "ERROR" "Failed to configure MinIO client. Check credentials."
  exit 1
fi

# Ensure bucket exists
log "INFO" "Creating bucket: ${MINIO_BUCKET:-bella-workflow}"

# Try to create the bucket and capture the output
output=$(mc mb myminio/${MINIO_BUCKET:-bella-workflow} 2>&1)
status=$?

# Check the result
if [ $status -eq 0 ]; then
  # Success case
  log "SUCCESS" "Bucket '${MINIO_BUCKET:-bella-workflow}' created successfully"
else
  # Check error message without using grep
  if [[ "$output" == *"already own it"* ]] || [[ "$output" == *"already exists"* ]]; then
    # This is not an error, bucket already exists
    log "INFO" "Bucket '${MINIO_BUCKET:-bella-workflow}' already exists"
  else
    # It's a different error
    log "ERROR" "Failed to create bucket '${MINIO_BUCKET:-bella-workflow}': $output"
    exit 1
  fi
fi

# Set bucket policy to allow downloads
log "INFO" "Setting bucket '${MINIO_BUCKET:-bella-workflow}' policy to allow downloads"
output=$(mc policy set download myminio/${MINIO_BUCKET:-bella-workflow} 2>&1)
if [ $? -eq 0 ]; then
  log "SUCCESS" "Bucket policy set successfully"
else
  log "WARN" "Failed to set bucket policy. Files may not be publicly accessible: $output"
fi

# Check if root path directory exists by trying to list it
mc_output=$(mc ls myminio/${MINIO_BUCKET:-bella-workflow}/${MINIO_ROOT_PATH:-test-states}/ 2>&1)
mc_status=$?

# Create root path directory if it doesn't exist
if [ $mc_status -ne 0 ] || [ -z "$mc_output" ]; then
  log "INFO" "Creating root path directory: ${MINIO_ROOT_PATH:-test-states}"
  
  # Create a temporary placeholder file
  echo "This is a placeholder file. Created by MinIO initialization script." > /tmp/placeholder.txt
  
  # Upload to specified path
  cp_output=$(mc cp /tmp/placeholder.txt myminio/${MINIO_BUCKET:-bella-workflow}/${MINIO_ROOT_PATH:-test-states}/.placeholder 2>&1)
  cp_status=$?
  
  if [ $cp_status -eq 0 ]; then
    log "SUCCESS" "Root path directory '${MINIO_ROOT_PATH:-test-states}' created successfully"
  else
    log "ERROR" "Failed to create root path directory '${MINIO_ROOT_PATH:-test-states}': $cp_output"
  fi
  
  # Remove temporary file
  rm /tmp/placeholder.txt
else
  log "INFO" "Root path directory '${MINIO_ROOT_PATH:-test-states}' already exists"
fi

# Print summary
log "SUCCESS" "MinIO initialization completed successfully"
log "INFO" "S3 endpoint: http://localhost:9000"
log "INFO" "Console URL: http://localhost:9001"
log "INFO" "Bucket: ${MINIO_BUCKET:-bella-workflow}"
log "INFO" "Root path: ${MINIO_ROOT_PATH:-test-states}"

# Keep the script running, don't exit the container
log "INFO" "Script will now keep the container running..."
tail -f /dev/null
