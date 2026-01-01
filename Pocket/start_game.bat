@echo off
title 宝可梦 MUD 客户端
echo ==========================================
echo       正在连接宝可梦世界...
echo ==========================================

REM 1. 编译客户端代码 (以防你修改了代码没编译)
REM 强制使用 UTF-8 读取源码，防止编译报错
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src/pokemon/GameClient.java

REM 2. 启动游戏客户端
echo.
echo 正在启动...
echo (请确保 run.bat 已经在另一个窗口运行中了！)
echo.

java -cp bin pokemon.GameClient

REM 3. 游戏退出后暂停，让你看清报错（如果有的话）
pause