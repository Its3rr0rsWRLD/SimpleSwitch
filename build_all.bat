@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo Multi-Version Plugin Builder (Concurrent)
echo ========================================================
echo.

:: Get the SS Build Version from gradle.properties
set "SS_VER=1.0.0"
if exist gradle.properties (
    for /f "tokens=1,2 delims==" %%a in (gradle.properties) do (
        if "%%a"=="version" set "SS_VER=%%b"
    )
)
:: Trim any possible spaces
set "SS_VER=!SS_VER: =!"

echo Detected SimpleSwitch version: !SS_VER!
echo.

:: Define the versions to build.
set "VERSIONS="

set "VERSIONS_17=1.19 1.19.1 1.19.2 1.19.3 1.19.4 1.20 1.20.1 1.20.2 1.20.3 1.20.4"
for %%v in (%VERSIONS_17%) do set "VERSIONS=!VERSIONS! %%v:17:%%v-R0.1-SNAPSHOT"

set "VERSIONS_21=1.20.5 1.20.6 1.21 1.21.1 1.21.3 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10 1.21.11"
for %%v in (%VERSIONS_21%) do set "VERSIONS=!VERSIONS! %%v:21:%%v-R0.1-SNAPSHOT"

set "VERSIONS_25=26.1.1 26.1.2 26.2"
for %%v in (%VERSIONS_25%) do set "VERSIONS=!VERSIONS! %%v:25:%%v.build.+"

set "OUT_DIR=build\libs\!SS_VER!"
if not exist "!OUT_DIR!" mkdir "!OUT_DIR!"
if not exist "build_logs" mkdir "build_logs"

:: Clean up old status files
del /Q .build_status_* >nul 2>&1
del /Q build_logs\run_*.bat >nul 2>&1

echo Starting concurrent builds...
echo Output logs for each build will be placed in the "build_logs" folder.
echo.

:: Count total versions
set "TOTAL_BUILDS=0"
for %%v in (%VERSIONS%) do set /a TOTAL_BUILDS+=1

set "CURRENT=0"
for %%v in (%VERSIONS%) do (
    for /f "tokens=1,2,3 delims=:" %%a in ("%%v") do (
        set /a CURRENT+=1
        set MC_VER=%%a
        set JAVA_VER=%%b
        set PAPER_VER=%%c
        
        echo [INFO] Spawning build !CURRENT!/!TOTAL_BUILDS! for Minecraft !MC_VER! (Java !JAVA_VER!)
        
        :: Create a sub-script for this specific build
        echo @echo off > "build_logs\run_!MC_VER!.bat"
        
        :: Run gradle with --project-cache-dir and custom buildDirName inside the build/ folder to avoid clutter
        echo call gradlew clean build -PmcVer=!MC_VER! -PjavaVer=!JAVA_VER! -PpaperVer=!PAPER_VER! -PbuildDirName=build/tmp_!MC_VER! --project-cache-dir build/.gradle_!MC_VER! --no-daemon ^> "build_logs\build_!MC_VER!.log" 2^>^&1 >> "build_logs\run_!MC_VER!.bat"
        
        :: Save the exit code to a status file (without trailing spaces)
        echo if errorlevel 1 ^(^>".build_status_!MC_VER!" echo 1^) else ^(^>".build_status_!MC_VER!" echo 0^) >> "build_logs\run_!MC_VER!.bat"
        echo exit >> "build_logs\run_!MC_VER!.bat"
        
        :: Start in background
        start "Build !MC_VER!" /b cmd /c "build_logs\run_!MC_VER!.bat"
    )
)

echo.
echo Waiting for all builds to finish...
echo.

:WAIT_LOOP
set "FINISHED_BUILDS=0"
for %%v in (%VERSIONS%) do (
    for /f "tokens=1,2,3 delims=:" %%a in ("%%v") do (
        if exist ".build_status_%%a" (
            set /a FINISHED_BUILDS+=1
        )
    )
)

:: Calculate percentage
set /a PERCENT=(FINISHED_BUILDS * 100) / TOTAL_BUILDS

:: Generate progress bar string (25 chars wide)
set /a BAR_FILLED=(PERCENT * 25) / 100
set /a BAR_EMPTY=25 - BAR_FILLED
set "BAR="
for /L %%i in (1,1,!BAR_FILLED!) do set "BAR=!BAR!#"
for /L %%i in (1,1,!BAR_EMPTY!) do set "BAR=!BAR!-"

:: Update window title
title SimpleSwitch Builder - !PERCENT!%% (!FINISHED_BUILDS!/!TOTAL_BUILDS!)

:: Print progress bar on the same line
powershell -NoProfile -Command "Write-Host -NoNewline \"`rProgress: [!BAR!] !FINISHED_BUILDS!/!TOTAL_BUILDS! (!PERCENT!%%) \""

if !FINISHED_BUILDS! LSS !TOTAL_BUILDS! (
    :: Wait 2 seconds before checking again
    timeout /t 2 /nobreak >nul
    goto WAIT_LOOP
)

echo.
echo.
title SimpleSwitch Builder - Done!
echo All builds finished!
echo.

set "NUM_SUCCESS=0"
set "NUM_FAILED=0"

echo ========================================================
echo Build Summary
echo ========================================================

for %%v in (%VERSIONS%) do (
    for /f "tokens=1,2,3 delims=:" %%a in ("%%v") do (
        set MC_VER=%%a
        
        :: Read status from file
        set /p STATUS=<.build_status_!MC_VER!
        
        if "!STATUS!"=="0" (
            echo [SUCCESS] !MC_VER!
            set /a NUM_SUCCESS+=1
            
            :: Copy jars from custom build dir
            for %%f in (build\tmp_!MC_VER!\libs\*-all.jar build\tmp_!MC_VER!\libs\*.jar) do (
                echo %%f | findstr /i /v "sources javadoc" >nul
                if !ERRORLEVEL! EQU 0 (
                    copy /Y "%%f" "!OUT_DIR!\SimpleSwitch-v!SS_VER!-!MC_VER!.jar" >nul
                )
            )
            
            :: Delete temporary build and cache folders for this version
            rmdir /s /q "build\tmp_!MC_VER!" >nul 2>&1
            rmdir /s /q "build\.gradle_!MC_VER!" >nul 2>&1
        ) else (
            echo [ERROR]   !MC_VER! - Check build_logs\build_!MC_VER!.log
            set /a NUM_FAILED+=1
            
            :: Clean up the temporary folders even on failure to avoid clutter
            rmdir /s /q "build\tmp_!MC_VER!" >nul 2>&1
            rmdir /s /q "build\.gradle_!MC_VER!" >nul 2>&1
        )
    )
)

:: Cleanup
del /Q .build_status_* >nul 2>&1
del /Q build_logs\run_*.bat >nul 2>&1

echo ========================================================
echo Successful Builds: !NUM_SUCCESS!
echo Failed Builds:     !NUM_FAILED!
echo Output Directory:  %CD%\!OUT_DIR!
echo ========================================================

if !NUM_FAILED! GTR 0 (
    echo [WARNING] Some builds failed. Please check the logs in the "build_logs" folder.
    exit /b 1
)

echo [SUCCESS] All builds completed successfully!
pause
exit /b 0
