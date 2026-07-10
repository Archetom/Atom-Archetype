# Release checklist

Atom Archetype publishes through the Sonatype Central Portal. The protected `Release to Maven Central` workflow automatically uploads, validates, publishes, and waits for the public deployment. Local `make deploy` keeps manual Portal approval as a recovery path.

## Prerequisites

- a Central Portal account authorized for `io.github.archetom`;
- Maven server credentials under server ID `central` in the user or CI settings;
- a signing key available to GnuPG and its full fingerprint supplied as `GPG_KEYNAME` when more than one key exists;
- Java 21 and the repository Maven wrapper;
- a clean Git worktree on the intended release commit.

Never store Central tokens, GPG private keys, or passphrases in the repository.

The GitHub `maven-central` Environment must contain these secrets:

- `CENTRAL_USERNAME`
- `CENTRAL_PASSWORD`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

## Prepare

1. Review `CHANGELOG.md`, compatibility documentation, and breaking changes.
2. Set a non-SNAPSHOT version:

   ```bash
   make version VERSION=2.0.0
   ```

3. Run the same gates used by CI:

   ```bash
   make install
   make demo
   cd ~/Downloads/atom-demo
   CI=true sh ./mvnw test
   ```

4. Confirm `target/` contains non-empty main, sources, and Javadoc JARs.
5. Commit the release version, wait for CI, and confirm the worktree is clean.

## Automated upload and publish

After the release commit is merged to `main` and CI passes, dispatch the protected workflow:

```bash
gh workflow run release.yml --ref main -f version=2.0.0
```

The workflow rejects non-`main` refs, SNAPSHOT versions, and input/POM version mismatches. It imports the signing key only for the hosted runner, generates Maven settings from Environment secrets, enables Central automatic publishing, and waits until Central reports `published`.

## Manual recovery path

```bash
GPG_KEYNAME='<full-key-fingerprint>' make deploy
```

The target refuses SNAPSHOT versions and dirty worktrees. Local publishing intentionally stops after validation. After the command reports that validation succeeded:

1. inspect the deployment in the Central Portal;
2. publish it explicitly;
3. verify the artifact and POM metadata in Central;
4. create and push the matching signed Git tag;
5. create the GitHub release from the reviewed changelog.

Central releases are immutable. If validation or verification fails, correct the project and publish a new version rather than attempting to replace an existing release.

## Continue development

Set the next snapshot only after the release is public:

```bash
make version VERSION=2.0.1-SNAPSHOT
```

Snapshot uploads use `make deploy-snapshot` and intentionally skip release signing and Portal publication.
