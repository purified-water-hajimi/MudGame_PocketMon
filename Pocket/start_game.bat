@echo off
title 宝可梦 MUD 客户端
echo ==========================================
echo       正在连接宝可梦世界...
echo ==========================================


if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src/pokemon/GameClient.java


echo.
echo 正在启动...
echo (请确保 run.bat 已经在另一个窗口运行中了！)
echo.

java -cp bin pokemon.GameClient


pause