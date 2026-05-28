# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack at a glance

OSIS is a **Java 17 Spring Boot 2.7.6** service built with **Gradle 7** as a multi-module project. Expect:

- `*.java` under `osis-app/`, `osis-core/`, `storage-platform-clients/`, and the root `src/main/java/`
- `*.gradle` build scripts (root `build.gradle`, per-module, plus `jacoco.gradle` / `pmd.gradle` / `spotbugs.gradle` / `upload-artifact.gradle`)
- `application.properties` and `crypto.yml` config templates under `src/main/resources/`
- Internal Scality dep: `com.scality:vaultclient` (Maven-published, pinned via `vaultclientVersion` in root `build.gradle`)
- External: AWS SDK (S3, STS, IAM), Spring Security + JWT, Spring Data Redis (Sentinel), SpringDoc OpenAPI

## What this service is

OSIS (Object Storage Interoperability Service) is the control-plane adapter that lets **VMware Cloud Director Object Storage Extension (OSE)** drive **Scality RING** for tenant/user/credential management. The data path stays between OSE and RING; OSIS only handles the control channel by translating OSE's REST contract into calls against Scality `Vault` (the Scality identity service, *not* HashiCorp Vault), S3, and Utapi.

See `Design.md` for the full per-API design (it is the source of truth for flow semantics like `AssumeRole`, `SetupAssumeRole`, and the `markerCache` / `assumeRoleCache` behaviors).

## Module layout

Gradle multi-project (`settings.gradle`) with three submodules **plus a root `src/`** that ships the Spring Boot entry point:

- `src/main/java/com/scality/osis/Application.java` — `@SpringBootApplication` main class. Referenced as `mainClass` in `build.gradle`'s `bootJar`. Not in any submodule.
- `src/main/resources/` — runtime config templates: `application.properties`, `crypto.yml`, `s3capabilities.json`. In the container these are expected at `/conf/application.properties` and `/conf/crypto.yml` (mounted as volumes; see `Dockerfile` `ENTRYPOINT`).
- `osis-app/` — REST controllers (`ScalityOsisController`) and Actuator health indicators (`S3HealthIndicator`, `VaultHealthIndicator`). Depends on `osis-core`.
- `osis-core/` — business logic: `service/`, `configuration/`, `security/` (JWT + basic + platform auth), `redis/`, `model/`, `resource/` (capabilities + Jackson customizer), `validation/`. Depends on `storage-platform-clients`.
- `storage-platform-clients/` — clients to Scality services: `vaultadmin/` (with `cache/`), `s3/`, `utapi/`, `utapiclient/`, plus `security/crypto/` for the secret-key encryption used to store credentials in Redis.

Dependency direction is strictly `osis-app → osis-core → storage-platform-clients`. Don't introduce reverse edges.

## Generated code: do not hand-edit

`osis-core/src/main/java/com/scality/osis/App.java` holds `VERSION` and `DATE` constants. The Gradle `app` task (`build.gradle:111`) rewrites both on every `compileJava` from `osisVersion` in `build.gradle:3` and the current time. To bump the version, change `osisVersion`, not `App.java`.

## Build, test, run

All commands assume the repo root and Java 17.

```sh
./gradlew clean build                # full build incl. JaCoCo + SpotBugs + PMD
./gradlew test                       # JUnit Platform tests (all modules)
./gradlew :osis-core:test --tests 'FullyQualifiedClassName.methodName'   # single test
./gradlew bootJar                    # executable jar at build/libs/osis-scality-<version>.jar
./gradlew dependencyCheckAnalyze     # OWASP dependency-check; report in build/reports
./gradlew spotbugsMain pmdMain       # static analysis (reports in reports/)
./gradlew jacocoTestReport           # coverage report → reports/code-coverage
```

Run as a standalone JAR (dev only):
```sh
java -jar -Dserver.tomcat.basedir=tomcat -Dserver.tomcat.accesslog.directory=logs \
     -Dserver.tomcat.accesslog.enabled=true \
     build/libs/osis-scality-<version>.jar
```

Docker build/run is documented in `README.md`. The image expects three volume mounts: the PKCS#12 keystore at `/app/lib/osis.p12`, plus `/conf/application.properties` and `/conf/crypto.yml`.

### Coverage gate

JaCoCo enforces a **75% minimum** via `jacocoTestCoverageVerification` (`jacoco.gradle:75`), wired into `check`. Drops below 75% fail the build. The verification excludes config classes, the legacy VMware OSIS stub, and large chunks of `com/scality/osis/security/**` — re-check the exclusion lists in `jacoco.gradle` before assuming a class is covered.

### PMD scope

`pmd.gradle:20` currently **excludes all of `com/scality/osis/**`** with a `Needs to be removed` comment. New Scality code is not actually PMD-gated yet; if you're tightening lint coverage, remove that exclusion and expect work.

## Architecture: the three flows you'll hit most

1. **Tenant APIs** use **super-admin Vault credentials** directly. `create-account` / `list-accounts` / `get-account` / `delete-account` against Vault. `list-accounts` results are paginated via a short-lived `markerCache` (60s) keyed by `(max-limit + 1)`.
2. **User and S3 credential APIs** require the **AssumeRole flow** first:
   - Look up `accountID → temporary creds` in `assumeRoleCache` (TTL 50 minutes; session token is valid 60). On miss, call `AssumeRoleBackbeat` as super-admin.
   - If that returns `NoSuchEntity / Role does not exist`, run the **`SetupAssumeRole` subroutine**: generate an account access key, create the `osis` role with the trust policy in `Design.md`, attach `adminPolicy@[account-id]` (full s3+iam), then delete the access key.
   - User-side flows also lazily create `userPolicy@[account-id]` (full s3) the first time a user gets an access denied.
3. **S3 credential storage**: secret keys are encrypted with the key from `crypto.yml` (`osis.security.keys.cipher`, 32-byte) and written to Redis Sentinel in the hash `osis:s3credentials` keyed by `<Username>__<AccessKeyID>`. The `storage-platform-clients/.../security/crypto` package owns the cipher.

The caches (`markerCache`, `assumeRoleCache`, `listAccountsCache`, `accountIDCache`) are all configured under `osis.scality.vault.cache.*` in `application.properties`. Look here first when investigating staleness or memory issues.

## Configuration surface

`src/main/resources/application.properties` is the canonical reference for runtime knobs. Notable groups:

- `osis.scality.vault.*` — Vault endpoint, super-admin keys, admin-creds decryption (`decrypt-admin-credentials=true` reads `admin-file-path` encrypted with `master-keyfile-path`), per-cache TTLs.
- `osis.scality.s3.*` / `osis.scality.utapi.*` — endpoints + healthcheck timeouts. Utapi healthcheck was removed in 2.2.5 (see commit `682ecc3`).
- `security.jwt.*` — JWT issuer, signing key, token TTLs. `security.jwt.enabled=false` by default; basic auth and platform auth live alongside under `osis-core/src/main/java/com/scality/osis/security/`.
- `spring.redis.sentinel.*` — required; the secret-key store assumes Sentinel.
- `management.endpoints.web.base-path=/_` and `management.endpoints.web.path-mapping.health=healthcheck` — health is served at `/_/healthcheck`, not the Spring default.

## CI

Workflows live under `.github/workflows/`:
- `test-and-build.yml` — runs on every push outside `development/**`, `main/**`, `q/*/**`. Calls `docker-build.yml` and uploads coverage to Codecov. **The Gradle build step was removed in commit `ea0ab90` (OSIS-152)** — Gradle now only runs inside the Docker build.
- `gradle-build-and-upload.yml` — invoked from `release.yml` (which is `workflow_dispatch`-only). Publishes signed snapshots/releases to Sonatype Nexus.
- `codeql.yaml`, `security.yaml`, `dependency-review.yaml` — security scans on PRs.

## When investigating a JIRA ticket against this repo

Follow the evidence-first workflow in `~/.claude/CLAUDE.md`. Repo-specific pointers:
- Bugs around tenant/user creation almost always trace to a Vault response — read the relevant `*ServiceImpl` in `osis-core/src/main/java/com/scality/osis/service/impl/` first, then the `vaultadmin` client implementation.
- Credential / login issues split between `osis-core/.../security/` (auth wiring) and `storage-platform-clients/.../security/crypto/` (encryption at rest).
- Caching-related bug? Confirm the relevant cache config in `application.properties` and the cache wrapper in `vaultadmin/impl/cache/` before suspecting business logic.
- The `Design.md` activity diagrams (under `Design_Files/`) are usually still accurate; cross-check ticket text against them.

## What not to do

- Don't hand-edit `osis-core/.../App.java` (it is generated).
- Don't add reverse-direction module deps (controllers → clients without going through core).
- Don't commit `application.properties` changes that bake in non-default endpoints/keys — the in-tree file is a template; real values land via the `/conf/` mount or env file at deploy time.
- The two encryption code paths (`security/crypto/Hkdf*`) are vendored from an external library and explicitly excluded from coverage/lint — don't refactor them as if they were ours.
