down-local-infra:
	docker-compose down --remove-orphans -v

up-local-infra:
	docker-compose down --remove-orphans -v\
    docker-compose build --no-cache\
    docker-compose up --force-recreate --renew-anon-volumes -d

build:
	./gradlew bootJar

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

helm-upgrade:
	@echo "Upgrading Helm release..."
	helm upgrade bank helm/bank --timeout 5m --wait

helm-rollback:
	@echo "Rolling back Helm release..."
	helm rollback bank
