#!/bin/bash
# Reinicio completo para desarrollo: resetea BD, aplica migraciones y arranca la API.

set -e

echo ">>> Eliminando base de datos..."
dotnet ef database drop \
  --project Needlos.Infraestructura \
  --startup-project Needlos.Api \
  --force 2>&1 | grep -v "NU1603"

echo ">>> Aplicando migraciones..."
dotnet ef database update \
  --project Needlos.Infraestructura \
  --startup-project Needlos.Api 2>&1 | grep -v "NU1603"

echo ">>> Iniciando API..."
dotnet run --project Needlos.Api 2>&1 | grep -v "NU1603"
