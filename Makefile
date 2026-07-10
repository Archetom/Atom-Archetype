MVN := ./mvnw
PROJECT_VERSION := $(shell awk -F'[<>]' '/^[[:space:]]*<version>/{print $$3; exit}' pom.xml)
VERSION ?= $(PROJECT_VERSION)
ARCHETYPE_PLUGIN_VERSION := $(shell awk -F'[<>]' '/<maven.archetype.packaging.version>/{print $$3; exit}' pom.xml)
VERSIONS_PLUGIN_VERSION := $(shell awk -F'[<>]' '/<versions.maven.plugin.version>/{print $$3; exit}' pom.xml)
GPG_KEY_ARGS = $(if $(strip $(GPG_KEYNAME)),-Dgpg.keyname=$(GPG_KEYNAME),)

.PHONY: default clean install release-check snapshot-check deploy deploy-snapshot version demo

default: install

clean:
	@$(MVN) clean

install:
	@$(MVN) clean install -Dgpg.skip=true

release-check:
	@case "$(PROJECT_VERSION)" in \
	  *-SNAPSHOT) echo "Error: release deployment requires a non-SNAPSHOT project version."; exit 1 ;; \
	esac
	@test -z "$$(git status --porcelain)" || { \
	  echo "Error: release deployment requires a clean Git worktree."; \
	  git status --short; \
	  exit 1; \
	}

snapshot-check:
	@case "$(PROJECT_VERSION)" in \
	  *-SNAPSHOT) ;; \
	  *) echo "Error: snapshot deployment requires a -SNAPSHOT project version."; exit 1 ;; \
	esac

deploy: release-check
	@$(MVN) clean deploy $(GPG_KEY_ARGS)
	@echo "Release deployment validated by Central; publish it explicitly in the Central Portal."

deploy-snapshot: snapshot-check
	@$(MVN) clean deploy -Dgpg.skip=true

version:
	@$(MVN) org.codehaus.mojo:versions-maven-plugin:$(VERSIONS_PLUGIN_VERSION):set \
	  -DgenerateBackupPoms=false \
	  -DnewVersion=$(VERSION)

demo: install
	cd ~/Downloads && rm -rf atom-demo && $(CURDIR)/mvnw -B -ntp \
	  org.apache.maven.plugins:maven-archetype-plugin:$(ARCHETYPE_PLUGIN_VERSION):generate \
	  -DarchetypeGroupId=io.github.archetom       \
	  -DarchetypeArtifactId=atom-archetype  \
	  -DarchetypeVersion=$(PROJECT_VERSION) \
	  -DgroupId=com.foo.bar                 \
	  -DartifactId=atom-demo      			\
	  -Dpackage=com.foo.bar                 \
	  -Dversion=1.0.0-SNAPSHOT
