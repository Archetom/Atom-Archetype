VERSION=1.0.0

default:install

clean:
	@mvn clean

install:clean
	@mvn install -U

deploy:clean
	@mvn deploy

package:install
	@mvn package -Dmaven.test.skip

native:clean
	@mvn native:compile -Pnative -DskipTests

version:
	@mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$(VERSION)
