#!/usr/bin/env bash
set -e

SOURCE="$0"
while [[ -h "$SOURCE"  ]]; do # resolve $SOURCE until the file is no longer a symlink
    scriptDir="$( cd -P "$( dirname "$SOURCE"  )" && pwd  )"
    SOURCE="$(readlink "$SOURCE")"
    [[ ${SOURCE} != /*  ]] && SOURCE="$scriptDir/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
scriptDir="$( cd -P "$( dirname "$SOURCE"  )" && pwd  )"
basedir=${scriptDir%/*}
echo "baseDir:"${basedir}
cd ${basedir}

PUBLISH_PROPS_FILE="${basedir}/.gradle/publish.properties"

if [[ ! -f "$PUBLISH_PROPS_FILE" ]];then
    echo "Error: $PUBLISH_PROPS_FILE not found. Please create it with sonatypeUsername and sonatypePassword."
    exit 1
fi

# Read credentials from the properties file
# Ensure we trim any potential whitespace/newlines
SONATYPE_USERNAME=$(grep "sonatypeUsername" "$PUBLISH_PROPS_FILE" | head -n 1 | cut -d'=' -f2 | tr -d '[:space:]')
SONATYPE_PASSWORD=$(grep "sonatypePassword" "$PUBLISH_PROPS_FILE" | head -n 1 | cut -d'=' -f2 | tr -d '[:space:]')

# Read signing properties
SIGNING_KEY_ID=$(grep "signing.keyId" "$PUBLISH_PROPS_FILE" | head -n 1 | cut -d'=' -f2 | tr -d '[:space:]')
SIGNING_PASSWORD=$(grep "signing.password" "$PUBLISH_PROPS_FILE" | head -n 1 | cut -d'=' -f2 | tr -d '[:space:]')
SIGNING_SECRET_KEY_RING_FILE=$(grep "signing.secretKeyRingFile" "$PUBLISH_PROPS_FILE" | head -n 1 | cut -d'=' -f2 | tr -d '[:space:]')


if [ -z "$SONATYPE_USERNAME" ] || [ "$SONATYPE_USERNAME" == "***" ]; then
    echo "Error: sonatypeUsername is not set or is default '***' in $PUBLISH_PROPS_FILE. Please edit the file."
    exit 1
fi

if [ -z "$SONATYPE_PASSWORD" ] || [ "$SONATYPE_PASSWORD" == "***" ]; then
    echo "Error: sonatypePassword is not set or is default '***' in $PUBLISH_PROPS_FILE. Please edit the file."
    exit 1
fi

if [ -z "$SIGNING_KEY_ID" ] || [ "$SIGNING_KEY_ID" == "***" ]; then
    echo "Error: signing.keyId is not set or is default '***' in $PUBLISH_PROPS_FILE. Please edit the file."
    exit 1
fi

if [ -z "$SIGNING_PASSWORD" ] || [ "$SIGNING_PASSWORD" == "***" ]; then
    echo "Error: signing.password is not set or is default '***' in $PUBLISH_PROPS_FILE. Please edit the file."
    exit 1
fi

if [ -z "$SIGNING_SECRET_KEY_RING_FILE" ] || [ "$SIGNING_SECRET_KEY_RING_FILE" == "***" ]; then
    echo "Error: signing.secretKeyRingFile is not set or is default '***' in $PUBLISH_PROPS_FILE. Please edit the file."
    exit 1
fi

echo "start clean"
rm -rf ${basedir}/build
./gradlew clean --stacktrace

echo "start publish"
./gradlew publish --stacktrace \
    -Psigning.keyId="$SIGNING_KEY_ID" \
    -Psigning.password="$SIGNING_PASSWORD" \
    -Psigning.secretKeyRingFile="$SIGNING_SECRET_KEY_RING_FILE"

# Get project version from gradle.properties
PROJECT_VERSION=$(grep "project_version" ${basedir}/gradle.properties | head -n 1 | cut -d'=' -f2 | tr -d '[:space:]')
if [ -z "$PROJECT_VERSION" ]; then
    echo "Error: project_version not found in gradle.properties"
    exit 1
fi
echo "Project Version: $PROJECT_VERSION"

# Create bundle.zip
echo "Creating bundle.zip..."
if [ -d "build/repo" ]; then
    # Check if zip is installed
    if ! command -v zip &> /dev/null; then
        echo "Error: 'zip' command not found. Please install zip."
        exit 1
    fi
    
    # Navigate to the repo directory to zip its contents
    cd build/repo
    rm -f ../../bundle.zip
    zip -r ../../bundle.zip .
    cd ../..
    echo "Bundle created at ${basedir}/bundle.zip"
else
    echo "Error: build/repo directory not found! Publication might have failed."
    exit 1
fi

# Generate Bearer Token
# Use python for consistent base64 encoding across platforms (avoiding newline issues)
AUTH_TOKEN=$(python3 -c "import base64; print(base64.b64encode(b'$SONATYPE_USERNAME:$SONATYPE_PASSWORD').decode('utf-8'))")

# Upload to Central Portal
echo "Uploading bundle to Central Portal..."
curl --request POST \
  --verbose \
  --header "Authorization: Bearer $AUTH_TOKEN" \
  --form bundle=@bundle.zip \
  --form publishingType=AUTOMATIC \
  --form name="com.itangcent.intellij-kotlin:$PROJECT_VERSION" \
  https://central.sonatype.com/api/v1/publisher/upload

echo "publish completed"
