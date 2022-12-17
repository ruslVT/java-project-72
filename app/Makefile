.DEFAULT_GOAL := build-run

clean:
	./gradlew clean

build:
	./gradlew clean build

install:
	./gradlew clean install

run-dist:
	APP_ENV=production ./build/install/app/bin/app

run:
	APP_ENV=development ./gradlew run

generate-migrations:
	./gradlew generateMigrations

test:
	./gradlew test

report:
	./gradlew jacocoTestReport

lint:
	./gradlew checkstyleMain checkstyleTest

build-run: build run

.PHONY: build
