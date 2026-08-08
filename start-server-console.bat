@echo off
@title MapleStory_079
pushd "%~dp0"
docker compose up -d --build ms079-server
if errorlevel 1 goto :error
docker compose logs -f ms079-server
popd
exit /b 0

:error
echo Failed to start the Docker server.
popd
pause
exit /b 1
