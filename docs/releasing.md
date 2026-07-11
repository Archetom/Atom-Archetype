# Release checklist

Atom Archetype publishes through the Sonatype Central Portal. The `Release to Maven Central` workflow publishes releases; `make deploy` is the manual recovery path.

## Prerequisites

- Central Portal access for `io.github.archetom`;
- Maven credentials under server ID `central`;
- a GnuPG signing key and, when needed, its full fingerprint in `GPG_KEYNAME`;
- Java 25 and the repository Maven wrapper;
- a clean worktree on the release commit.

Do not store Central tokens, GPG private keys, or passphrases in the repository.

The GitHub `maven-central` Environment contains:

- `CENTRAL_USERNAME`
- `CENTRAL_PASSWORD`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

Snapshot publishing requires **Enable SNAPSHOTs** for the `io.github.archetom` namespace in the [Central Portal namespace settings](https://central.sonatype.com/publishing/namespaces). Snapshot jobs use `CENTRAL_USERNAME` and `CENTRAL_PASSWORD` only.

## Prepare a release

1. Review `CHANGELOG.md`, compatibility notes, and breaking changes.
2. Set a non-SNAPSHOT version:

   ```bash
   make version VERSION=2.0.0
   ```

3. Run the CI gates:

   ```bash
   make install
   make demo
   cd ~/Downloads/atom-demo
   CI=true sh ./mvnw test
   ```

4. Confirm `target/` contains non-empty main, sources, and Javadoc JARs.
5. Commit the version, merge it to `main`, and wait for CI.

## Publish a release

```bash
gh workflow run release.yml --ref main -f version=2.0.0
```

The workflow accepts only `main`, a non-SNAPSHOT version that matches the POM, and a successful signed Central deployment. It waits until Central reports `published`.

## Publish a snapshot

After the intended `-SNAPSHOT` commit is merged to `main` and CI passes, run:

```bash
gh workflow run snapshot.yml --ref main -f version=2.1.0-SNAPSHOT
```

The snapshot workflow accepts only `main`, a SNAPSHOT version that matches the POM, and uses the existing Portal token without GPG signing. Release and snapshot workflows share a concurrency group.

Portal snapshots are mutable and are currently removed by Sonatype after approximately 90 days. Consumers must enable the snapshot repository:

```xml
<repositories>
  <repository>
    <id>central-portal-snapshots</id>
    <name>Central Portal Snapshots</name>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <releases>
      <enabled>false</enabled>
    </releases>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```

Use `-U` to resolve the latest timestamped build. To verify with an empty Maven local repository:

```bash
temporary_repository="$(mktemp -d)"
./mvnw -B -U -ntp dependency:get \
  -Dmaven.repo.local="$temporary_repository" \
  -Dartifact=io.github.archetom:atom-archetype:2.1.0-SNAPSHOT \
  -Dtransitive=false \
  -DremoteRepositories=central-portal-snapshots::default::https://central.sonatype.com/repository/maven-snapshots/
```

## Manual recovery

```bash
GPG_KEYNAME='<full-key-fingerprint>' make deploy
```

The target rejects SNAPSHOT versions and dirty worktrees. It uploads and validates the deployment but leaves Portal publication for manual approval.

After validation:

1. inspect and publish the deployment in Central Portal;
2. verify the artifact and POM metadata;
3. create and push the matching signed Git tag;
4. create the GitHub release from the reviewed changelog.

Central releases are immutable. Publish a new version if validation or verification fails after release.

## Continue development

Set the next snapshot after the release is public:

```bash
make version VERSION=2.0.1-SNAPSHOT
```

Local snapshot uploads use `make deploy-snapshot` and skip release signing and Portal publication.
