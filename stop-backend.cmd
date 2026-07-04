@echo off
chcp 65001 >nul
echo 正在查找占用 8080 端口的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo 停止进程 PID: %%a
    taskkill /F /PID %%a >nul 2>&1
)
echo 8080 端口已释放，可以重新运行 start-backend.cmd
pause
