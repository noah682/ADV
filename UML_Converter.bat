@echo off
cd /d "%~dp0"
start "" "C:\Program Files\Java\jdk-21\bin\javaw.exe" ^
    --module-path "app\lib" ^
    --add-modules javafx.controls,javafx.graphics,javafx.base ^
    -cp "app\lib\javafx-base-21.0.4-win.jar;app\lib\javafx-graphics-21.0.4-win.jar;app\lib\javafx-controls-21.0.4-win.jar;app\classes" ^
    UmlLauncherApp
