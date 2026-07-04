@echo off
chcp 65001 >nul
cd /d "%~dp0backend"
set "JAVA_HOME=D:\develop\Java\jdk-21"
set "MAVEN_HOME=D:\download\maven\apache-maven-3.9.11-bin\apache-maven-3.9.11"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;D:\node;%PATH%"
echo 正在启动后端 http://localhost:8080 ...
"%MAVEN_HOME%\bin\mvn.cmd" spring-boot:run
pause
