#!/usr/bin/env bash
set -euo pipefail

# Script to setup port-forwarding for integration tests
# Forwards PostgreSQL (5432) and Keycloak (8180) from Kubernetes to localhost

echo "Setting up port-forwarding for integration tests..."

# Kill any existing port-forward processes
pkill -f "kubectl port-forward.*postgresql" 2>/dev/null || true
pkill -f "kubectl port-forward.*keycloak" 2>/dev/null || true

sleep 1

# Forward PostgreSQL (5432)
echo "Forwarding PostgreSQL: localhost:5432 -> postgresql:5432"
kubectl port-forward svc/postgresql 5432:5432 &
PG_PID=$!

# Forward Keycloak (8180)
echo "Forwarding Keycloak: localhost:8180 -> keycloak:8080"
kubectl port-forward svc/keycloak 8180:8080 &
KC_PID=$!

echo ""
echo "Port-forwarding started:"
echo "  PostgreSQL PID: $PG_PID"
echo "  Keycloak PID: $KC_PID"
echo ""
echo "To stop port-forwarding, run:"
echo "  kill $PG_PID $KC_PID"
echo ""
echo "Waiting for connections..."

# Wait for both processes
wait
