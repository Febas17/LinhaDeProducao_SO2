@echo off
chcp 65001 >nul
title Linha de Producao - Interface Grafica (SO II)

echo ========================================================
echo   LINHA DE PRODUCAO CONCORRENTE - SISTEMAS OPERACIONAIS II
echo   Compilando e abrindo a Interface Grafica Pixel Art...
echo ========================================================
echo.

javac -encoding UTF-8 *.java
if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Falha na compilacao! Verifique se o JDK esta no PATH.
    echo.
    pause
    exit /b %errorlevel%
)

java FabricaGUI
