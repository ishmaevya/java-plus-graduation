#!/usr/bin/env bash

echo "=== Очистка Docker ==="
docker-compose down -v --remove-orphans

echo "=== Сборка Maven ==="
mvn clean package -DskipTests

echo "=== Сборка Docker Compose ==="
docker compose build --no-cache

echo "=== Запуск Docker Compose ==="
docker compose up -d
docker compose logs -f