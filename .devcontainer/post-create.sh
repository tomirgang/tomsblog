#!/bin/bash
set -e

echo "=== Post-Create: Python venv + Doorstop ==="
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt

echo "=== Post-Create: Infrastruktur starten ==="
docker compose -f infra/docker/docker-compose.yml up -d

echo "=== Post-Create: PlantUML Server starten ==="
docker run -d --name plantuml -p 8180:8080 plantuml/plantuml-server:jetty

echo "=== Post-Create: Maven Dependencies cachen ==="
./mvnw dependency:go-offline -q || true

echo "=== Fertig! ==="
echo "Java:    $(java -version 2>&1 | head -1)"
echo "Node:    $(node --version)"
echo "Python:  $(python3 --version)"
echo "Doorstop: $(doorstop --version 2>/dev/null || echo 'in .venv')"
