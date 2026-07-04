# 启动前端（自动加载 Node 环境）
$nodeHome = "D:\node"
$env:Path = "$nodeHome;" + $env:Path

Set-Location $PSScriptRoot\frontend
Write-Host "正在启动前端 http://localhost:5173 ..." -ForegroundColor Cyan
npm run dev
