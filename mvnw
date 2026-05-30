#!/bin/sh

set -e

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_DIR="$SCRIPT_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

mkdir -p "$WRAPPER_DIR"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Downloading Maven wrapper JAR..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
  else
    echo "Error: curl or wget is required to download $WRAPPER_JAR"
    exit 1
  fi
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_EXE="$JAVA_HOME/bin/java"
else
  JAVA_EXE="java"
fi

exec "$JAVA_EXE" -classpath "$WRAPPER_JAR" -Dmaven.multiModuleProjectDirectory="$SCRIPT_DIR" org.apache.maven.wrapper.MavenWrapperMain "$@"
