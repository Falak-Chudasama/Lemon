#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)

if [ -n "$JAVA_HOME" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi

if ! command -v "$JAVA_CMD" >/dev/null 2>&1; then
  echo "ERROR: Java is required to run Gradle." >&2
  exit 1
fi

WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Gradle wrapper JAR is missing. Open this project in Android Studio or install Gradle 8.9 and run: gradle wrapper --gradle-version 8.9" >&2
  exit 1
fi

exec "$JAVA_CMD" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
