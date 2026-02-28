#!/bin/bash
# ==============================================================================
# Script para actualizar un documento PENDING en MongoDB.
# Simula una actualización externa que desbloquea el controller.
#
# Uso: ./update-mongo.sh <REQUEST_ID>
# ==============================================================================

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "❌ Uso: $0 <REQUEST_ID>"
    echo ""
    echo "   El REQUEST_ID se muestra en los logs de la app al hacer la petición."
    echo "   Ejemplo: $0 65f1a2b3c4d5e6f7a8b9c0d1"
    exit 1
fi

REQUEST_ID="$1"
CONTAINER_NAME="poc-mongo"

echo "📝 Actualizando documento con ID: ${REQUEST_ID}"
echo "   → status: COMPLETED"
echo "   → result: Respuesta procesada externamente"
echo ""

docker exec "${CONTAINER_NAME}" mongosh poc_reactive --quiet --eval "
  var result = db.pendingRequests.updateOne(
    { _id: ObjectId('${REQUEST_ID}') },
    {
      \$set: {
        status: 'COMPLETED',
        result: 'Respuesta procesada externamente desde script'
      }
    }
  );
  printjson(result);
"

echo ""
echo "✅ Documento actualizado. El controller debería responder ahora."
