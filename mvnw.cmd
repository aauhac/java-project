@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

set "WRAPPER_DIR=%SCRIPT_DIR%\.mvn\wrapper"
set "WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar"
set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if not exist "%WRAPPER_DIR%" (
  mkdir "%WRAPPER_DIR%"
)

if not exist "%WRAPPER_JAR%" (
  echo Downloading Maven wrapper JAR...
  where powershell >nul 2>nul
  if %ERRORLEVEL% EQU 0 (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
  ) else (
    where curl >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
      curl -f -L -o "%WRAPPER_JAR%" "%WRAPPER_URL%"
    )
  )
)

if not exist "%WRAPPER_JAR%" (
  echo Failed to download %WRAPPER_JAR%
  exit /b 1
)

if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java"
)

"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%SCRIPT_DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
set "MVNW_EXIT=%ERRORLEVEL%"

endlocal & exit /b %MVNW_EXIT%
