#set( $dollar = '$' )
MVN ?= sh ./mvnw
VERSION ?= ${version}

.PHONY: default clean compile test integration-test verify install package run infra-up infra-down clean-sample version

default: verify

clean:
	@${dollar}(MVN) clean

compile:
	@${dollar}(MVN) compile

test:
	@CI=false ${dollar}(MVN) test

integration-test:
	@CI=true ${dollar}(MVN) test

verify:
	@CI=false ${dollar}(MVN) verify

install:
	@CI=false ${dollar}(MVN) clean install

package:
	@CI=false ${dollar}(MVN) clean package

run:
	@${dollar}(MVN) -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev

infra-up:
	@docker compose up -d mysql redis

infra-down:
	@docker compose down

clean-sample:
	@bash clean.sh

version:
	@${dollar}(MVN) versions:set -DgenerateBackupPoms=false -DnewVersion=${dollar}(VERSION)
