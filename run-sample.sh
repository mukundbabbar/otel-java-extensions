#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

# Local convenience only. Production should already have these in the process environment.
# .env.example is committed (includes JAVA_TOOL_OPTIONS). .env is gitignored and overlays secrets.
set -a
if [[ -f "$ROOT/.env.example" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.env.example"
fi
if [[ -f "$ROOT/.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.env"
fi
set +a

# Splunk Cloud token header. Skip for local Observer / collector.
if [[ -z "${OTEL_EXPORTER_OTLP_HEADERS:-}" && -n "${SPLUNK_ACCESS_TOKEN:-}" ]]; then
  endpoints="${OTEL_EXPORTER_OTLP_ENDPOINT:-}${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:-}"
  case "${endpoints}" in
    *127.0.0.1*|*localhost*) ;;
    *) export OTEL_EXPORTER_OTLP_HEADERS="X-SF-TOKEN=${SPLUNK_ACCESS_TOKEN}" ;;
  esac
fi

# Default sample-app extractor config if not already set.
if [[ -z "${INSTRUMENTATION_CONFIG:-}" ]]; then
  export INSTRUMENTATION_CONFIG="$ROOT/java-extension/src/main/resources/instlocal.json"
fi
if [[ "${INSTRUMENTATION_CONFIG}" != /* ]]; then
  export INSTRUMENTATION_CONFIG="$ROOT/${INSTRUMENTATION_CONFIG}"
fi

if [[ -z "${SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE:-}" ]]; then
  export SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE="$ROOT/java-extension/src/main/resources/nocode.yml"
fi
if [[ "${SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE}" != /* ]]; then
  export SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE="$ROOT/${SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE}"
fi

EXTENSION_JAR="$ROOT/java-extension/build/libs/otel-java-extension-1.0-all.jar"
APP_JAR="$ROOT/sample-app/target/sample-apps.jar"
AGENT_VERSION="2.31.1"
# No-code YAML is a Splunk Java agent feature. Vanilla OTel agent ignores it.
AGENT_JAR="$ROOT/.otel/splunk-otel-javaagent.jar"
AGENT_URL="https://repo1.maven.org/maven2/com/splunk/splunk-otel-javaagent/${AGENT_VERSION}/splunk-otel-javaagent-${AGENT_VERSION}.jar"

if [[ ! -f "$EXTENSION_JAR" ]]; then
  echo "Build the extension first: (cd java-extension && ./gradlew assemble)" >&2
  exit 1
fi

if [[ ! -f "$APP_JAR" ]]; then
  echo "Package the sample app first: (cd sample-app && mvn -DskipTests package)" >&2
  exit 1
fi

if [[ ! -f "$AGENT_JAR" ]]; then
  mkdir -p "$ROOT/.otel"
  curl -fsSL "$AGENT_URL" -o "$AGENT_JAR"
fi

exec java \
  -javaagent:"$AGENT_JAR" \
  -Dotel.javaagent.extensions="$EXTENSION_JAR" \
  -jar "$APP_JAR"
