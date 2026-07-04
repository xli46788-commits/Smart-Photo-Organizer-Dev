# 以管理员身份运行此脚本启动 MySQL
# 右键 PowerShell → 以管理员身份运行 → 执行:
#   Set-ExecutionPolicy -Scope Process Bypass -Force
#   .\start-mysql.ps1

$serviceName = "MySQL80"

Write-Host "正在启动 MySQL 服务 ($serviceName)..." -ForegroundColor Cyan

$service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if (-not $service) {
    Write-Host "未找到 MySQL 服务，请检查 MySQL 是否已安装。" -ForegroundColor Red
    exit 1
}

if ($service.Status -eq 'Running') {
    Write-Host "MySQL 已在运行。" -ForegroundColor Green
} else {
    Start-Service $serviceName
    Start-Sleep -Seconds 3
    $service.Refresh()
    if ($service.Status -eq 'Running') {
        Write-Host "MySQL 启动成功！" -ForegroundColor Green
    } else {
        Write-Host "MySQL 启动失败，请打开「服务」手动启动 MySQL80。" -ForegroundColor Red
        exit 1
    }
}

$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$sqlFile = Join-Path $PSScriptRoot "sql\init.sql"

Write-Host "正在初始化数据库 imprint..." -ForegroundColor Cyan
Write-Host "请输入 MySQL root 密码（application.yml 中默认为 root）:" -ForegroundColor Yellow
& $mysqlBin -u root -p -e "source $sqlFile"

Write-Host "完成！请确认 backend/src/main/resources/application.yml 中的数据库密码正确。" -ForegroundColor Green
