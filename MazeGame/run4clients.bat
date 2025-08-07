@echo off
echo Starting 4 JavaFX clients...

start "Client 1" cmd /k "mvn javafx:run"
start "Client 2" cmd /k "mvn javafx:run"
start "Client 3" cmd /k "mvn javafx:run"
start "Client 4" cmd /k "mvn javafx:run"

echo All clients started!
pause