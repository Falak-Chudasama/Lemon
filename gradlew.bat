@echo off
set DIR=%~dp0
if not defined JAVA_HOME set JAVA_CMD=java
if defined JAVA_HOME set JAVA_CMD=%JAVA_HOME%\bin\java.exe
if not exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  echo Gradle wrapper JAR is missing. Open this project in Android Studio or run "gradle wrapper --gradle-version 8.9".
  exit /b 1
)
"%JAVA_CMD%" -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
