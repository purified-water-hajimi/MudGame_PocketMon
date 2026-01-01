@echo off
echo ==========================================
echo       正在编译服务器代码...
echo ==========================================

REM 创建存放编译文件的文件夹
if not exist bin mkdir bin

REM 核心：编译时强制指定 UTF-8，确保不会报“编码错误”
javac -encoding UTF-8 -d bin src/pokemon/*.java

REM 检查有没有编译报错
if %errorlevel% neq 0 (
    echo.
    echo ❌ 编译失败！请检查代码错误。
    pause
    exit /b
)

echo.
echo ==========================================
echo       编译成功！正在启动服务器...
echo       (注意：如果本窗口显示乱码请忽略，不影响游戏！)
echo ==========================================

REM 启动服务器
java -cp bin pokemon.GameServer

pause