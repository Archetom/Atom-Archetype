VERSION=1.1.0

default:install

clean:
	@mvn clean

install:clean
	@mvn install -U -Dgpg.skip=true

deploy:clean
	@mvn deploy

version:
	@mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$(VERSION)

demo:install
	cd ~/Downloads && rm -rf atom-demo && mvn archetype:generate  \
	  -DarchetypeGroupId=io.github.archetom       \
	  -DarchetypeArtifactId=atom-archetype  \
	  -DarchetypeVersion=$(VERSION)         \
	  -DgroupId=com.foo.bar                 \
	  -DartifactId=atom-demo      			\
	  -Dpackage=com.foo.bar                 \
	  -Dversion=1.0.0-SNAPSHOT              \
	  -B
