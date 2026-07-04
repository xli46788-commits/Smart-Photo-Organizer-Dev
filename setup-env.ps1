# IMPrint 开发环境配置脚本
# 用法: 在 PowerShell 中执行  .\setup-env.ps1

$javaHome = "D:\develop\Java\jdk-21"
$mavenHome = "D:\download\maven\apache-maven-3.9.11-bin\apache-maven-3.9.11"
$nodeHome = "D:\node"

$env:JAVA_HOME = $javaHome
$env:MAVEN_HOME = $mavenHome
$env:Path = "$javaHome\bin;$mavenHome\bin;$nodeHome;" + $env:Path

Write-Host "=== 环境已加载 ===" -ForegroundColor Green
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "MAVEN_HOME: $env:MAVEN_HOME"
Write-Host ""
java -version
Write-Host ""
mvn -version
Write-Host ""
node -v
npm -v
