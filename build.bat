@echo off
rem Compile the whole project into the out\ directory (Windows CMD / PowerShell: .\build.bat).
if exist out rmdir /s /q out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
set RESULT=%ERRORLEVEL%
del sources.txt
if %RESULT% neq 0 (echo Build FAILED) else (echo Build OK - classes are in out\)
exit /b %RESULT%
