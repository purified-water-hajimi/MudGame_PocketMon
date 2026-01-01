@echo off
echo ==========================================
echo       正在编译游戏代码，请稍候...
echo ==========================================

cd src\pokemon
javac -encoding UTF-8 *.java

echo.
echo ==========================================
echo       编译完成！正在启动服务器...
echo ==========================================

cd ..
java pokemon.GameMain

pause