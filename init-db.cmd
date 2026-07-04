@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 正在初始化数据库 imprint ...
echo 请输入 MySQL root 密码后回车：
mysql -u root -p < sql\init.sql
if %errorlevel% equ 0 (
    echo 数据库初始化成功！
) else (
    echo 初始化失败，请检查 MySQL 是否运行、密码是否正确。
)
pause
