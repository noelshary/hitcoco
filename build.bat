@echo off
REM Baut die HitColor-Mod. Fertige Datei: build\libs\hitcolor-1.0.0.jar
if exist gradlew.bat (
    call gradlew.bat build
) else (
    call gradle build
)
echo.
echo Fertig. Die Mod liegt in build\libs\  (die Datei OHNE "-sources")
pause
