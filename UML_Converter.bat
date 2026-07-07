@echo off
cd /d "%~dp0"
rem Gebuendeltes JDK verwenden - keine Java-Installation auf dem Rechner noetig.
rem PATH wird ergaenzt, damit auch der von der GUI gestartete "java"-Subprozess
rem (Konvertierung) das mitgelieferte Java findet.
set "PATH=%~dp0app\jdk\bin;%PATH%"
start "" "%~dp0app\jdk\bin\javaw.exe" ^
    --module-path "app\lib" ^
    --add-modules javafx.controls,javafx.graphics,javafx.base ^
    -cp "app\lib\javafx-base-21.0.4-win.jar;app\lib\javafx-graphics-21.0.4-win.jar;app\lib\javafx-controls-21.0.4-win.jar;app\classes" ^
    UmlLauncherApp
