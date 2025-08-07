@echo off
echo Starting 4 JavaFX clients...
start "Client 1" cmd /k "mvn javafx:run"
start "Client 2" cmd /k "mvn javafx:run"
start "Client 3" cmd /k "mvn javafx:run"
start "Client 4" cmd /k "mvn javafx:run"
echo All clients started!

echo.
echo Press any key to close all clients, or close this window to leave them running...
pause >nul

echo Closing all clients...
taskkill /f /fi "WindowTitle eq Client 1*" >nul 2>&1
taskkill /f /fi "WindowTitle eq Client 2*" >nul 2>&1
taskkill /f /fi "WindowTitle eq Client 3*" >nul 2>&1
taskkill /f /fi "WindowTitle eq Client 4*" >nul 2>&1
echo All clients closed!
pause