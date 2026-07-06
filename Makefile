# Build
build:
	./gradlew bootJar

# Build Docker images (automatically available in Colima)
docker-build:
	docker build -t bank-accounts:latest -f Dockerfile . --build-arg SERVICE_NAME=accounts
	docker build -t bank-cash:latest -f Dockerfile . --build-arg SERVICE_NAME=cash
	docker build -t bank-transfer:latest -f Dockerfile . --build-arg SERVICE_NAME=transfer
	docker build -t bank-notifications:latest -f Dockerfile . --build-arg SERVICE_NAME=notifications
	docker build -t bank-gateway:latest -f Dockerfile . --build-arg SERVICE_NAME=gateway
	docker build -t bank-frontend:latest -f Dockerfile . --build-arg SERVICE_NAME=frontend

# Kubernetes deployment
k8s-deploy:
	helm upgrade --install bank helm/bank -f helm/values-dev.yaml -f helm/values-secret.yaml --timeout 5m --wait

k8s-rollback:
	helm rollback bank

k8s-status:
	kubectl get pods
	kubectl get svc

k8s-logs:
	kubectl logs -l app=accounts -f

k8s-delete:
	helm uninstall bank

# Port forwarding
k8s-port-forward:
	@echo "Starting port-forwarding..."
	kubectl port-forward svc/frontend 32190:8080 &
	kubectl port-forward svc/postgresql 5432:5432 &
	kubectl port-forward svc/keycloak 8180:8080
	@echo "Port-forwarding started:"
	@echo "  Frontend: http://localhost:32190"
	@echo "  PostgreSQL: localhost:5432"
	@echo "  Keycloak: http://localhost:8180"

# Helm tests
helm-lint:
	@echo "Running Helm lint..."
	helm lint helm/accounts
	helm lint helm/cash
	helm lint helm/transfer
	helm lint helm/notifications
	helm lint helm/gateway
	helm lint helm/frontend
	helm lint helm/postgresql
	helm lint helm/keycloak
	helm lint helm/bank
	@echo "All Helm charts passed lint!"

helm-unit-test:
	@echo "Running Helm unit tests..."
	helm unittest helm/accounts helm/cash helm/transfer helm/notifications helm/gateway helm/frontend -f 'tests/*_test.yaml'
	@echo "All Helm unit tests passed!"

helm-test: helm-lint helm-unit-test
	@echo "All Helm tests passed!"

helm-template:
	@echo "Rendering Helm templates..."
	helm template bank helm/bank --debug

# Full local development cycle
dev: build docker-build k8s-deploy
	@echo "Development environment ready!"
	@echo "Frontend: http://localhost:32190"
	@echo "Keycloak Admin: http://localhost:8180 (admin/admin)"

# Test all
test:
	./gradlew test contractTest
	@echo "All tests passed!"

.PHONY: build docker-build k8s-deploy k8s-rollback k8s-status k8s-logs k8s-delete k8s-port-forward helm-lint helm-unit-test helm-test helm-template dev test
