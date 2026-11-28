@echo off
chcp 65001 >nul
title Linha de Producao - Modo Terminal (SO II)

echo ========================================================
echo   LINHA DE PRODUCAO CONCORRENTE - SISTEMAS OPERACIONAIS II
echo   Professor: Victor Lattari ^| Integrante: Eduardo Fadini
echo ========================================================
echo.
echo Escolha o cenario de execucao:
echo.
echo   [1] Cenario 1: Fluxo Completo (Caso Inicial - 24 pecas)
echo   [2] Cenario 2: Armazenamento Reduzido (Filas com Capacidade 1)
echo   [3] Cenario 3: Inspecao Lenta (Gargalo I1/I2)
echo   [4] Cenario 4: Disputa por Exaustor (600ms em S1 e P1)
echo   [5] Cenario 5: Carga Ampliada (48 pecas - Fator 0.1 Turbo)
echo   [G] Abrir Interface Grafica Pixel Art (FabricaGUI)
echo   [S] Sair
echo.

set /p cenario="Digite a opcao desejada [1-5, G ou S] (padrao: 1): "

if "%cenario%"=="" set cenario=1
if /i "%cenario%"=="s" exit /b
if /i "%cenario%"=="g" (
    echo.
    echo Abrindo interface grafica...
    javac -encoding UTF-8 *.java
    java FabricaGUI
    exit /b
)

echo.
echo Compilando arquivos Java...
javac -encoding UTF-8 *.java
if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Falha na compilacao!
    pause
    exit /b %errorlevel%
)

cls
echo ========================================================
echo   EXECUTANDO CENARIO %cenario% NO TERMINAL...
echo ========================================================
echo.

java Fabrica %cenario%

echo.
echo ========================================================
echo   Execucao finalizada. Pressione qualquer tecla para sair.
echo ========================================================
pause >nul
