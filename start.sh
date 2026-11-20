#!/usr/bin/env bash
set -euo pipefail

MVN="${MVN:-mvn}"

echo "=== Очистка Docker ==="
docker compose down -v --remove-orphans

echo "=== Сборка Maven ==="
"$MVN" clean package -DskipTests

echo "=== Сборка Docker Compose ==="
docker compose build --no-cache

echo "=== Запуск Docker Compose ==="
docker compose up -d
docker compose ps
docker compose logs -f
