#!/bin/bash

# Gitleaks security scan script
# This script performs security scanning to detect potential secrets in code

set -e

echo "🔍 Running Gitleaks security scan..."

# Check if gitleaks is installed
if ! command -v gitleaks &> /dev/null; then
    echo "⚠️  Gitleaks not found. Installing..."

    # Install gitleaks based on OS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if command -v brew &> /dev/null; then
            brew install gitleaks
        else
            echo "❌ Homebrew not found. Please install gitleaks manually:"
            echo "   Visit: https://github.com/gitleaks/gitleaks#installation"
            exit 1
        fi
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        echo "Please install gitleaks manually for Linux:"
        echo "   Visit: https://github.com/gitleaks/gitleaks#installation"
        exit 1
    else
        echo "❌ Unsupported OS. Please install gitleaks manually:"
        echo "   Visit: https://github.com/gitleaks/gitleaks#installation"
        exit 1
    fi
fi

# Run gitleaks scan on staged files
echo "Scanning staged files for secrets..."

# Create a temporary config file if it doesn't exist
GITLEAKS_CONFIG=""
if [ -f ".gitleaks.toml" ]; then
    GITLEAKS_CONFIG="--config=.gitleaks.toml"
fi

# Scan staged files
if ! git diff --cached | gitleaks stdin --verbose --exit-code 1; then
    echo ""
    echo "❌ Gitleaks found potential secrets in your staged files!"
    echo "   Please review and remove any sensitive information before committing."
    echo "   You can use 'git diff --cached' to see your staged changes."
    echo ""
    echo "   If this is a false positive, you can:"
    echo "   1. Add the file/pattern to .gitleaks.toml allowlist"
    echo "   2. Or skip this check with: git commit --no-verify"
    echo ""
    exit 1
fi

echo "✅ Gitleaks scan completed successfully - no secrets detected!"
