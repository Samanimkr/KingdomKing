@echo off
call "%~dp0build.bat" run %*
if errorlevel 1 pause
