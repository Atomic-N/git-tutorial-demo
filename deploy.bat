@echo off
chcp 65001 > nul
echo ========================================
echo AI TEF 论坛部署脚本
echo ========================================
echo.

REM 1. 清理旧构建
echo [1/3] 清理旧构建产物...
call mvn clean
if %ERRORLEVEL% neq 0 (
    echo 错误: Maven clean 失败
    pause
    exit /b 1
)

REM 2. 打包项目
echo.
echo [2/3] 打包项目...
call mvn package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo 错误: Maven package 失败
    pause
    exit /b 1
)

REM 3. 部署到 Tomcat
echo.
echo [3/3] 部署到 Tomcat...
set TOMCAT_WEBAPPS=%CATALINA_HOME%\webapps
if "%TOMCAT_WEBAPPS%"=="" (
    echo 警告: 未设置 CATALINA_HOME 环境变量
    echo 请手动复制 target\ai-tef-forum.war 到 Tomcat webapps 目录
    pause
    exit /b 0
)

copy /Y target\ai-tef-forum.war "%TOMCAT_WEBAPPS%\ai-tef-forum.war"
if %ERRORLEVEL% neq 0 (
    echo 错误: 复制 WAR 文件失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo 部署完成！
echo ========================================
echo 访问地址: http://localhost:8080/ai-tef-forum/latest
echo.
pause
