#!/bin/bash
BUILD_DIR="build"

# Check if the build directory exists
if [[ ! -d "$BUILD_DIR" ]]; then
  echo "❌ Build directory not found: $BUILD_DIR"
  exit 1
fi
echo "📂 Using build directory: $BUILD_DIR"

# Get the uuid of the latest build
UUID_PROPS_FILE="$BUILD_DIR/generated/honeycomb/proguard-uuid.properties"
if [[ ! -f "$UUID_PROPS_FILE" ]]; then
  echo "❌ UUID properties file not found: $UUID_PROPS_FILE"
  exit 1
fi
echo "📝 Found UUID properties file: $UUID_PROPS_FILE"

UUID=$(grep "^io\.honeycomb\.proguard\.uuid=" "$UUID_PROPS_FILE" | cut -d'=' -f2)
if [[ -z "$UUID" ]]; then
  echo "❌ UUID not found in properties file"
  exit 1
fi
echo "🔑 Found UUID: $UUID"

# Check for the ProGuard mapping file
PROGUARD_MAPPING_FILE="$BUILD_DIR/outputs/mapping/release/mapping.txt"
if [[ ! -f "$PROGUARD_MAPPING_FILE" ]]; then
  echo "❌ ProGuard mapping file not found: $PROGUARD_MAPPING_FILE"
  exit 1
fi
echo "📝 Found ProGuard mapping file: $PROGUARD_MAPPING_FILE"

# Upload the mapping file to S3 with the UUID as the file name
# aws s3 cp "$PROGUARD_MAPPING_FILE" s3://my-app-artifacts/android/$UUID.txt
