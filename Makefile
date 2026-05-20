down-local-infra:
	docker-compose down --remove-orphans -v

up-local-infra:
	docker-compose down --remove-orphans -v\
    docker-compose build --no-cache\
    docker-compose up --force-recreate --renew-anon-volumes -d

build:
	./gradlew bootJar