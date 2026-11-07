@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
title Warehouse Inventory Management

rem ================================================================
rem  One-click launcher: finds a JDK, builds the project, runs the menu.
rem  Just double-click this file.  Optional: run.bat path\to\data_folder
rem ================================================================

set "JBIN="

rem 1) JAVA_HOME
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JBIN=%JAVA_HOME%\bin\"
if defined JBIN goto found

rem 2) javac found on PATH
for /f "delims=" %%i in ('where javac 2^>nul') do if not defined JBIN set "JBIN=%%~dpi"
if defined JBIN goto found

rem 3) common JDK install folders
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-*" "%ProgramFiles%\Java\jdk*" "%ProgramFiles%\Microsoft\jdk-*" "%ProgramFiles%\Amazon Corretto\jdk*" "%ProgramFiles%\Zulu\zulu*" "%ProgramFiles%\BellSoft\LibericaJDK-*") do if exist "%%~d\bin\javac.exe" set "JBIN=%%~d\bin\"
if defined JBIN goto found

echo [ERROR] No JDK found. Install JDK 11 or newer, for example Temurin from https://adoptium.net
echo         then double-click this file again.
goto fail

:found
rem Use the java.exe and javac.exe of the SAME JDK (avoids UnsupportedClassVersionError)
set "PATH=%JBIN%;%PATH%"

set "JV=0"
for /f "tokens=2 delims=. " %%v in ('javac -version 2^>^&1') do set "JV=%%v"
set /a JVN=JV
if %JVN% LSS 11 goto oldjava

echo Using JDK: %JBIN%
echo Building...
if exist out rmdir /s /q out
javac -encoding UTF-8 -d out -sourcepath src\main\java src\main\java\warehouse\app\WarehouseApp.java
if errorlevel 1 goto fail

echo Build OK. Starting the system...
echo.
java -cp out warehouse.app.WarehouseApp %*
echo.
pause
exit /b 0

:oldjava
echo [ERROR] The JDK found (%JBIN%) is older than version 11. Install JDK 11 or newer.
goto fail

:fail
echo.
pause
exit /b 1
