@echo off
cd /d "%~dp0"
rem Nutzt das auf dem Rechner installierte Java (JDK 21 oder neuer erforderlich).
rem JavaFX liegt im Projekt unter app\lib und muss nicht installiert werden.
start "" javaw ^
    --module-path "app\lib" ^
    --add-modules javafx.controls,javafx.graphics,javafx.base ^
    -cp "app\lib\javafx-base-21.0.4-win.jar;app\lib\javafx-graphics-21.0.4-win.jar;app\lib\javafx-controls-21.0.4-win.jar;app\classes" ^
    UmlLauncherApp
