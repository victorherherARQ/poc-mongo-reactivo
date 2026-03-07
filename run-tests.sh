#!/bin/bash

# Aseguramos que existe el directorio para los reportes
mkdir -p performance-reports

echo "=== Configuración de la Prueba de Rendimiento ==="

# Solicitamos parametros al usuario
read -p "Introduce el numero de usuarios concurrentes (hilos) [50]: " VUS
VUS=${VUS:-50}

read -p "Introduce el tiempo de rampa (ej. 30s, 1m) [30s]: " RAMP_TIME
RAMP_TIME=${RAMP_TIME:-30s}

echo ""
echo "Iniciando prueba con $VUS VUs y tiempo de rampa $RAMP_TIME..."
echo ""

# Ejecutamos k6 pasando las variables de entorno
# Se asume que k6 está en performance/k6-v0.50.0-linux-amd64/k6 o en el PATH
if [ -f "performance/k6-v0.50.0-linux-amd64/k6" ]; then
    K6_BIN="performance/k6-v0.50.0-linux-amd64/k6"
else
    # Fallback por si k6 estuviera instalado globalmente
    K6_BIN="k6"
fi

VUS=$VUS RAMP_TIME=$RAMP_TIME $K6_BIN run performance/load-test.js

# Si se generó el resumen en markdown, lo movemos a su carpeta con la fecha
if [ -f "summary.md" ]; then
    DATE_FORMAT=$(date +"%Y-%m-%d_%H-%M-%S")
    REPORT_NAME="performance-reports/report_$DATE_FORMAT.md"
    mv summary.md "$REPORT_NAME"
    echo ""
    echo "✅ ¡Reporte markdown generado exitosamente en: $REPORT_NAME!"
fi
