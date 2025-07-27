@echo off
title EmoteStream Setup Script
color 0a

echo ========================================
echo ===   EmoteStream Install Wizard     ===
echo ========================================
echo.

:: STEP 1 – Check if Python is installed
python --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [!] Python not found! Descarca de aici: https://www.python.org/downloads/
    echo [!] Cand instalezi, asigura-te ca bifezi: "Add Python to PATH"
    pause
    exit /b
) ELSE (
    echo [✔] Python este instalat.
)

:: STEP 2 – Check if python is in PATH
where python >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [!] Python nu este in PATH.
    echo [!] Va trebui sa il adaugi manual sau sa reinstalezi Python cu optiunea "Add to PATH".
    pause
    exit /b
) ELSE (
    echo [✔] Python este in PATH.
)

:: STEP 3 – Creează mediu virtual
echo.
echo [•] Creez mediu virtual...
python -m venv venv
IF EXIST venv\Scripts\activate.bat (
    echo [✔] Mediu virtual creat.
) ELSE (
    echo [!] Eroare la creare mediu virtual.
    pause
    exit /b
)

:: STEP 4 – Activează mediu virtual
echo [•] Activez mediu virtual...
call venv\Scripts\activate.bat

:: STEP 5 – Instalează dependențele
echo.
echo [•] Instalez pachetele necesare...
pip install --upgrade pip
pip install -r requirements.txt

IF %ERRORLEVEL% NEQ 0 (
    echo [!] Eroare la instalarea dependintelor!
    pause
    exit /b
) ELSE (
    echo [✔] Toate pachetele au fost instalate cu succes!
)

:: STEP 6 – Rulează aplicația
echo.
echo [•] Pornesc aplicatia...
python main.py

:: Gata!
echo.
echo [✔] Totul este gata. Aplicatia ruleaza!
pause
