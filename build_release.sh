#!/bin/bash
set -e
cd "$(dirname "$0")"
if [ ! -f release.jks ]; then
  echo "release.jks not found in project root" >&2
  exit 1
fi
if [ -z "$RELEASE_STORE_PASSWORD" ] || [ -z "$RELEASE_KEY_PASSWORD" ]; then
  echo "set RELEASE_STORE_PASSWORD and RELEASE_KEY_PASSWORD" >&2
  exit 1
fi
./gradlew :app:assembleRelease :app:bundleRelease \
  -PreleaseStoreFile=release.jks \
  -PreleaseStorePassword="$RELEASE_STORE_PASSWORD" \
  -PreleaseKeyAlias="${RELEASE_KEY_ALIAS:-unicon}" \
  -PreleaseKeyPassword="$RELEASE_KEY_PASSWORD"

# Gradle writes to app/build/outputs/...; publish a copy to app/release/
# so the artifacts are easy to find.
mkdir -p app/release
cp app/build/outputs/bundle/release/app-release.aab app/release/
cp app/build/outputs/apk/release/app-release.apk app/release/
if [ -f app/build/outputs/mapping/release/mapping.txt ]; then
  cp app/build/outputs/mapping/release/mapping.txt app/release/
fi
echo "Wrote:"
ls -lh app/release/app-release.aab app/release/app-release.apk
