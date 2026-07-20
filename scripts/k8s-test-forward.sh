#!/usr/bin/env bash
set -euo pipefail

# Script to setup port-forwarding for integration tests
# Forwards PostgreSQL (5432), Keycloak (8180), Zipkin (9411), Prometheus (9090), Kibana (30561), and Grafana (30030)

echo "Setting up port-forwarding for integration tests..."

# Kill any existing port-forward processes
pkill -f "kubectl port-forward.*postgresql" 2>/dev/null || true
pkill -f "kubectl port-forward.*keycloak" 2>/dev/null || true
pkill -f "kubectl port-forward.*zipkin" 2>/dev/null || true
pkill -f "kubectl port-forward.*prometheus" 2>/dev/null || true
pkill -f "kubectl port-forward.*kibana" 2>/dev/null || true
pkill -f "kubectl port-forward.*grafana" 2>/dev/null || true

sleep 1

# Forward PostgreSQL (5432)
echo "Forwarding PostgreSQL: localhost:5432 -> postgresql:5432"
kubectl port-forward svc/postgresql 5432:5432 &
PG_PID=$!

# Forward Keycloak (8180)
echo "Forwarding Keycloak: localhost:8180 -> keycloak:8080"
kubectl port-forward svc/keycloak 8180:8080 &
KC_PID=$!

# Forward Zipkin (9411)
echo "Forwarding Zipkin: localhost:9411 -> zipkin:9411"
kubectl port-forward svc/zipkin 9411:9411 &
ZK_PID=$!

# Forward Prometheus (9090)
echo "Forwarding Prometheus: localhost:9090 -> prometheus:9090"
kubectl port-forward svc/prometheus 9090:9090 &
PM_PID=$!

# Forward Kibana (30561)
echo "Forwarding Kibana: localhost:30561 -> kibana:5601"
kubectl port-forward svc/kibana 30561:5601 &
KB_PID=$!

# Forward Grafana (30030)
echo "Forwarding Grafana: localhost:30030 -> grafana:3000"
kubectl port-forward svc/grafana 30030:3000 &
GF_PID=$!

echo ""
echo "Port-forwarding started:"
echo "  PostgreSQL PID: $PG_PID"
echo "  Keycloak PID: $KC_PID"
echo "  Zipkin PID: $ZK_PID"
echo "  Prometheus PID: $PM_PID"
echo "  Kibana PID: $KB_PID"
echo "  Grafana PID: $GF_PID"
echo ""
echo "To stop port-forwarding, run:"
echo "  kill $PG_PID $KC_PID $ZK_PID $PM_PID $KB_PID $GF_PID"
echo ""
echo "Access URLs:"
echo "  PostgreSQL: localhost:5432"
echo "  Keycloak: http://localhost:8180"
echo "  Zipkin: http://localhost:9411"
echo "  Prometheus: http://localhost:9090"
echo "  Kibana: http://localhost:30561 (admin/admin)"
echo "  Grafana: http://localhost:30030 (admin/admin)"
echo ""
echo "Waiting for connections..."

# Wait for all processes
wait
