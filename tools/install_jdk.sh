#!/usr/bin/env bash
set -Eeuo pipefail
mkdir -p ci-output
exec > >(tee ci-output/jdk-install.log) 2>&1
stage=initialization
trap 'code=$?; printf "::error title=JDK installation::Failed at stage %s (exit %s). See jdk-install.log.\n" "$stage" "$code"; exit "$code"' ERR
jdk_archive="$RUNNER_TEMP/earthward-jdk.tar.gz"
jdk_home="$RUNNER_TEMP/earthward-jdk"
stage=download
curl --fail --silent --show-error --location --retry 3 --max-time 300 \
  'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz' \
  --output "$jdk_archive"
stage=checksum
printf '%s  %s\n' 'ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94' "$jdk_archive" | sha256sum --check --strict
stage=extraction
mkdir -p "$jdk_home"
tar -xzf "$jdk_archive" --directory "$jdk_home" --strip-components=1
stage=runtime
"$jdk_home/bin/java" -XshowSettings:properties -version 2>&1 | tee ci-output/jdk-version.log
stage=version_validation
runtime_version=$(sed -n 's/^[[:space:]]*java.runtime.version = //p' ci-output/jdk-version.log | tr -d '\r')
case "$runtime_version" in
  '21.0.12.1+1'|'21.0.12.1+1-LTS') ;;
  *) printf '::error title=JDK version mismatch::Expected 21.0.12.1+1 with optional -LTS label; inspect jdk-version.log.\n'; exit 1 ;;
esac
stage=compiler
"$jdk_home/bin/javac" -version
stage=environment
printf 'JAVA_HOME=%s\n' "$jdk_home" >> "$GITHUB_ENV"
printf '%s/bin\n' "$jdk_home" >> "$GITHUB_PATH"
printf '::notice title=JDK installed::Checksum verified; Temurin runtime %s.\n' "$runtime_version"
