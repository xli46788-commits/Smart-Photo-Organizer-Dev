# 启动后端（自动加载 Java/Maven 环境）
$javaHome = "D:\develop\Java\jdk-21"
$mavenHome = "D:\download\maven\apache-maven-3.9.11-bin\apache-maven-3.9.11"
$nodeHome = "D:\node"

$env:JAVA_HOME = $javaHome
$env:MAVEN_HOME = $mavenHome
$env:Path = "$javaHome\bin;$mavenHome\bin;$nodeHome;" + $env:Path

Set-Location $PSScriptRoot\backend
Write-Host "正在启动后端 http://localhost:8080 ..." -ForegroundColor Cyan
mvn spring-boot:run
