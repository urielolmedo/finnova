@echo off
REM ============================================================
REM FinNova - Script de arranque (Windows)
REM Levanta PostgreSQL (si no esta corriendo), el backend
REM (Spring Boot) y el frontend (React + Vite), cada uno en su
REM propia ventana.
REM ============================================================

REM Ruta raiz del proyecto. Si tu carpeta no es esta, cambiala aca:
set PROYECTO_DIR=%USERPROFILE%\proyectos\finnova

set BACKEND_DIR=%PROYECTO_DIR%\finnova-backend
set FRONTEND_DIR=%PROYECTO_DIR%\finnova-frontend

echo ==============================================
echo  FinNova - Arrancando entorno de desarrollo
echo ==============================================

REM --- 1. Verificar que las carpetas existan ---
if not exist "%BACKEND_DIR%" (
    echo ERROR: no se encontro la carpeta del backend en %BACKEND_DIR%
    echo Revisa la variable PROYECTO_DIR al principio de este script.
    pause
    exit /b 1
)

if not exist "%FRONTEND_DIR%" (
    echo ERROR: no se encontro la carpeta del frontend en %FRONTEND_DIR%
    echo Revisa la variable PROYECTO_DIR al principio de este script.
    pause
    exit /b 1
)

REM --- 2. Verificar / arrancar el servicio de PostgreSQL ---
echo.
echo --^> Verificando PostgreSQL...

REM El nombre del servicio suele ser algo como "postgresql-x64-18".
REM Ajusta POSTGRES_SERVICE si el tuyo se llama distinto
REM (podes verlo en el Administrador de Servicios de Windows, buscando "postgresql").
set POSTGRES_SERVICE=postgresql-x64-18

sc query "%POSTGRES_SERVICE%" | find "RUNNING" >nul
if %errorlevel%==0 (
    echo     PostgreSQL ya esta corriendo.
) else (
    echo     PostgreSQL no esta activo. Intentando iniciarlo...
    net start "%POSTGRES_SERVICE%"
    if errorlevel 1 (
        echo     No se pudo iniciar el servicio automaticamente.
        echo     Abri el Administrador de Servicios de Windows ^(services.msc^),
        echo     busca el servicio de PostgreSQL y arrancalo manualmente,
        echo     o revisa que el nombre configurado en este script sea correcto.
    ) else (
        echo     PostgreSQL iniciado correctamente.
    )
)

REM --- 3. Levantar el backend en una ventana nueva ---
echo.
echo --^> Levantando el backend (Spring Boot) en una ventana nueva...
start "FinNova - Backend" cmd /k "cd /d %BACKEND_DIR% && mvnw.cmd spring-boot:run"

REM Le damos unos segundos de ventaja al backend antes de levantar el frontend
timeout /t 3 /nobreak >nul

REM --- 4. Levantar el frontend en otra ventana nueva ---
echo.
echo --^> Levantando el frontend (Vite) en una ventana nueva...
start "FinNova - Frontend" cmd /k "cd /d %FRONTEND_DIR% && npm run dev"

echo.
echo ==============================================
echo  Listo. Se abrieron 2 ventanas nuevas:
echo    - Backend:  http://localhost:8080
echo    - Frontend: http://localhost:5173
echo.
echo  Backend  tarda unos segundos en arrancar del todo.
echo  Frontend deberia estar disponible casi al instante.
echo ==============================================
pause
