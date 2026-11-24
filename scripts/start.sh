#!/usr/bin/env bash
# Builds the Book Store and starts it on Payara Micro 7 (Linux, macOS, Git Bash).
# The shop opens at http://localhost:8080/ ; stop it with Ctrl+C.
#
#   ./scripts/start.sh               build, then start
#   ./scripts/start.sh --skip-build  start what was built last time
#
# Needs JDK 21: set JAVA_HOME, or have java 21 on the PATH.
set -euo pipefail
cd "$(dirname "$0")/.."

java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
version="$("$java" -version 2>&1 | head -n 1)"
if ! [[ "$version" =~ \"(2[1-9]|[3-9][0-9]) ]]; then
    echo "JDK 21 or newer is needed; found: $version. Set JAVA_HOME to a JDK 21 folder." >&2
    exit 1
fi

if [[ "${1:-}" != "--skip-build" ]]; then
    echo "Building (tests skipped)..."
    ./mvnw -B -ntp -q install -DskipTests
fi

echo 'Starting Payara Micro; the shop opens at http://localhost:8080/ once it says "ready".'
exec ./mvnw -B -ntp -pl app -Ppayara package exec:exec -DskipTests
