@echo off
chcp 65001 >nul
cd /d "%~dp0frontend"
set "PATH=D:\node;%PATH%"
echo 正在启动前端 http://localhost:5173 ...
"D:\node\npm.cmd" run dev
pause
