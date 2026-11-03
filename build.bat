@echo off
echo Building Clinic Patient Flow Board...
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src\clinicflow\*.java src\clinicflow\model\*.java src\clinicflow\service\*.java src\clinicflow\storage\*.java src\clinicflow\ui\*.java src\clinicflow\util\*.java src\clinicflow\test\*.java
if %ERRORLEVEL% EQU 0 (
    echo =======================================
    echo   Build Successful!
    echo =======================================
) else (
    echo =======================================
    echo   Build Failed!
    echo =======================================
)
