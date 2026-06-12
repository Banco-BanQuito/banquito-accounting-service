-include .env
export

MVN := ./mvnw

.PHONY: sonar-up sonar-down clean compile test build analyze

sonar-up:
	docker compose -f docker-compose-sonar.yml up -d

sonar-down:
	docker compose -f docker-compose-sonar.yml down

clean:
	$(MVN) clean

compile:
	$(MVN) compile

test:
	$(MVN) test

build:
	$(MVN) clean package -DskipTests

analyze:
	$(MVN) verify sonar:sonar -Dsonar.token=$(SONAR_TOKEN)
