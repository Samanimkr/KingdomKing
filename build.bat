@echo off
cd /d "%~dp0"
if defined JAVA_HOME (
  "%JAVA_HOME%\bin\java.exe" tools\Build.java %*
) else (
  java tools\Build.java %*
)
exit /b %errorlevel%
