# 连接本地 MySQL 数据库（交互式输入密码）
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

if (-not (Test-Path $mysql)) {
    Write-Host "未找到 MySQL，请检查安装路径。" -ForegroundColor Red
    exit 1
}

$service = Get-Service MySQL80 -ErrorAction SilentlyContinue
if ($service -and $service.Status -ne 'Running') {
    Write-Host "MySQL80 未运行。请先启动：services.msc → MySQL80 → 启动" -ForegroundColor Yellow
    exit 1
}

Write-Host "=== IMPrint 本地数据库 ===" -ForegroundColor Cyan
Write-Host "连接: localhost:3306  用户: root  库: imprint"
Write-Host "请输入 MySQL root 密码后回车：" -ForegroundColor Yellow
Write-Host ""

& $mysql -u root -p
