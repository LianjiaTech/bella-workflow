#!/bin/bash

# Multi-architecture Docker build script for bella-workflow API
# Supports both amd64 and arm64 architectures

set -e

# Default values
IMAGE_NAME="bella-workflow-api"
TAG="latest"
PUSH=false
PLATFORMS="linux/amd64,linux/arm64"
BUILDER_NAME="bella-multiarch-builder"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--name)
            IMAGE_NAME="$2"
            shift 2
            ;;
        -t|--tag)
            TAG="$2"
            shift 2
            ;;
        --push)
            PUSH=true
            shift
            ;;
        --platforms)
            PLATFORMS="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -n, --name NAME        Image name (default: bella-workflow-api)"
            echo "  -t, --tag TAG          Image tag (default: latest)"
            echo "  --push                 Push to registry after build"
            echo "  --platforms PLATFORMS  Target platforms (default: linux/amd64,linux/arm64)"
            echo "  -h, --help             Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                           # Build for amd64+arm64 (cache only)"
            echo "  $0 --push                   # Build and push multi-arch to registry"
            echo "  $0 --platforms linux/amd64  # Build single arch and load locally"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "=== Multi-Architecture Docker Build ==="
echo "Image: ${IMAGE_NAME}:${TAG}"
echo "Platforms: ${PLATFORMS}"
echo "Push: ${PUSH}"
echo ""

# Check if Docker buildx is available
if ! docker buildx version >/dev/null 2>&1; then
    echo "Error: Docker buildx is not available. Please update Docker to a version that supports buildx."
    exit 1
fi

# Create or use existing builder instance
echo "Setting up buildx builder..."
if ! docker buildx inspect ${BUILDER_NAME} >/dev/null 2>&1; then
    echo "Creating new buildx builder: ${BUILDER_NAME}"
    docker buildx create --name ${BUILDER_NAME} --driver docker-container --bootstrap
else
    echo "Using existing buildx builder: ${BUILDER_NAME}"
fi

# Use the builder
docker buildx use ${BUILDER_NAME}

# Build command
BUILD_CMD="docker buildx build"
BUILD_CMD="${BUILD_CMD} --platform ${PLATFORMS}"
BUILD_CMD="${BUILD_CMD} --tag ${IMAGE_NAME}:${TAG}"

# Check if building for multiple platforms
PLATFORM_COUNT=$(echo ${PLATFORMS} | tr ',' '\n' | wc -l)

if [ "${PUSH}" = true ]; then
    BUILD_CMD="${BUILD_CMD} --push"
elif [ ${PLATFORM_COUNT} -eq 1 ]; then
    # Only use --load for single platform builds
    BUILD_CMD="${BUILD_CMD} --load"
else
    # For multi-platform builds without push, build to local registry
    echo "Warning: Multi-platform builds cannot be loaded locally without pushing."
    echo "Building to buildx cache only. Use --push to push to registry."
fi

BUILD_CMD="${BUILD_CMD} ."

echo "Executing: ${BUILD_CMD}"
echo ""

# Execute the build
eval ${BUILD_CMD}

echo ""
echo "Build completed successfully!"

if [ "${PUSH}" = true ]; then
    echo "Image pushed to registry: ${IMAGE_NAME}:${TAG}"
else
    echo "Image built locally: ${IMAGE_NAME}:${TAG}"
    echo "To push to registry, run with --push flag"
fi

# Show built images
echo ""
echo "Available images:"
docker buildx imagetools inspect ${IMAGE_NAME}:${TAG} 2>/dev/null || echo "Use 'docker images' to see local images"
