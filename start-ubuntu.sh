#!/bin/bash
# ============================================================
# FinNova - Script de arranque (Ubuntu)
# Levanta PostgreSQL (si no esta corriendo), el backend
# (Spring Boot) y el frontend (React + Vite), cada uno en su
# propia terminal.
# ============================================================

# Ruta raiz del proyecto. Si tu carpeta no es esta, cambiala aca:
PROYECTO_DIR="$HOME/proyectos/finnova"

BACKEND_DIR="$PROYECTO_DIR/finnova-backend"
FRONTEND_DIR="$PROYECTO_DIR/finnova-frontend"

echo "=============================================="
echo " FinNova - Arrancando entorno de desarrollo"
echo "=============================================="

# --- 1. Verificar que las carpetas existan ---
if [ ! -d "$BACKEND_DIR" ]; then
    echo "ERROR: no se encontro la carpeta del backend en $BACKEND_DIR"
    echo "Revisa la variable PROYECTO_DIR al principio de este script."
    exit 1
fi

if [ ! -d "$FRONTEND_DIR" ]; then
    echo "ERROR: no se encontro la carpeta del frontend en $FRONTEND_DIR"
    echo "Revisa la variable PROYECTO_DIR al principio de este script."
    exit 1
fi

# --- 2. Verificar / arrancar PostgreSQL ---
echo ""
echo "--> Verificando PostgreSQL..."
if systemctl is-active --quiet postgresql; then
    echo "    PostgreSQL ya esta corriendo."
else
    echo "    PostgreSQL no esta activo. Intentando iniciarlo (te va a pedir tu password de sudo)..."
    sudo systemctl start postgresql
    if systemctl is-active --quiet postgresql; then
        echo "    PostgreSQL iniciado correctamente."
    else
        echo "    ERROR: no se pudo iniciar PostgreSQL. Revisalo manualmente con:"
        echo "    sudo systemctl status postgresql"
        exit 1
    fi
fi

# --- 3. Levantar el backend en una terminal nueva ---
echo ""
echo "--> Levantando el backend (Spring Boot) en una terminal nueva..."

if command -v gnome-terminal &> /dev/null; then
    gnome-terminal --title="FinNova - Backend" -- bash -c "cd '$BACKEND_DIR' && ./mvnw spring-boot:run; exec bash"
elif command -v konsole &> /dev/null; then
    konsole --new-tab -p tabtitle="FinNova - Backend" -e bash -c "cd '$BACKEND_DIR' && ./mvnw spring-boot:run; exec bash"
elif command -v xterm &> /dev/null; then
    xterm -T "FinNova - Backend" -e bash -c "cd '$BACKEND_DIR' && ./mvnw spring-boot:run; exec bash" &
else
    echo "    No se encontro una terminal grafica conocida (gnome-terminal/konsole/xterm)."
    echo "    Abri una terminal manualmente y corre:"
    echo "    cd $BACKEND_DIR && ./mvnw spring-boot:run"
fi

# Le damos unos segundos de ventaja al backend antes de levantar el frontend
sleep 3

# --- 4. Levantar el frontend en otra terminal nueva ---
echo ""
echo "--> Levantando el frontend (Vite) en una terminal nueva..."

if command -v gnome-terminal &> /dev/null; then
    gnome-terminal --title="FinNova - Frontend" -- bash -c "cd '$FRONTEND_DIR' && npm run dev; exec bash"
elif command -v konsole &> /dev/null; then
    konsole --new-tab -p tabtitle="FinNova - Frontend" -e bash -c "cd '$FRONTEND_DIR' && npm run dev; exec bash"
elif command -v xterm &> /dev/null; then
    xterm -T "FinNova - Frontend" -e bash -c "cd '$FRONTEND_DIR' && npm run dev; exec bash" &
else
    echo "    No se encontro una terminal grafica conocida (gnome-terminal/konsole/xterm)."
    echo "    Abri otra terminal manualmente y corre:"
    echo "    cd $FRONTEND_DIR && npm run dev"
fi

echo ""
echo "=============================================="
echo " Listo. Se abrieron 2 terminales nuevas:"
echo "   - Backend:  http://localhost:8080"
echo "   - Frontend: http://localhost:5173"
echo ""
echo " Backend  tarda unos segundos en arrancar del todo."
echo " Frontend deberia estar disponible casi al instante."
echo "=============================================="
