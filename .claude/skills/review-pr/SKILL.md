---
name: review-pr
description: Review a PR on osis (Java 17 / Spring Boot control-plane adapter that bridges VMware Cloud Director OSE and Scality RING via Vault, S3, and Utapi)
argument-hint: <pr-number-or-url>
disable-model-invocation: true
allowed-tools: Read, Bash(gh repo view *), Bash(gh pr view *), Bash(gh pr diff *), Bash(gh pr comment *), Bash(gh api *), Bash(git diff *), Bash(git log *), Bash(git show *)
---

# Review GitHub PR

You are an expert code reviewer. Review this PR: $ARGUMENTS

## Determine PR target

Parse `$ARGUMENTS` to extract the repo and PR number:

- If arguments contain `REPO:` and `PR_NUMBER:` (CI mode), use those values directly.
- If the argument is a GitHub URL (starts with `https://github.com/`), extract `owner/repo` and the PR number from it.
- If the argument is just a number, use the current repo from `gh repo view --json nameWithOwner -q .nameWithOwner`.

## Output mode

- **CI mode** (arguments contain `REPO:` and `PR_NUMBER:`): post inline comments and summary to GitHub.
- **Local mode** (all other cases): output the review as text directly. Do NOT post anything to GitHub.

## Steps

1. **Fetch PR details:**

```bash
gh pr view <number> --repo <owner/repo> --json title,body,headRefOid,author,files
gh pr diff <number> --repo <owner/repo>
```

2. **Read changed files** to understand the full context around each change (not just the diff hunks). For OSIS specifically:
   - If the PR touches `osis-core/src/main/java/com/scality/osis/service/impl/*ServiceImpl.java`, also read the corresponding `storage-platform-clients/.../vaultadmin/impl/` or `s3/impl/` client. Service-level bugs almost always trace back to a Vault/S3 response shape.
   - If the PR touches anything under `osis-core/.../security/`, also read `storage-platform-clients/.../security/crypto/` — the auth wiring and the at-rest crypto are intentionally split.
   - `Design.md` and the diagrams under `Design_Files/` are the source of truth for `AssumeRole`, `SetupAssumeRole`, `markerCache`, and `assumeRoleCache` semantics. Cross-check behavior changes against them.

3. **Analyze the changes** against these criteria:

| Area | What to check |
|------|---------------|
| Generated code | `osis-core/src/main/java/com/scality/osis/App.java` is rewritten by the `app` Gradle task on every `compileJava`. Hand-edits to `VERSION` or `DATE` will be silently clobbered. Version bumps must change `osisVersion` in root `build.gradle:3`, not `App.java`. |
| Module dependency direction | Strict: `osis-app → osis-core → storage-platform-clients`. Flag any new dependency that reverses this (e.g. a controller importing a client directly, or `osis-core` referencing `osis-app`). |
| Spring patterns | `@Autowired` field injection vs constructor injection — prefer constructor injection for new beans. `@Async` methods must not call other `@Async` methods on the same bean (self-invocation bypasses the proxy). Bean scope changes are breaking. |
| Exception handling | No swallowed exceptions (`catch (Exception e) {}` with no log/rethrow). Vault/S3/STS SDK errors should be mapped to the appropriate `OsisException` subtype in `model/exception/`, not leaked raw. `NoSuchEntity` from `AssumeRoleBackbeat` has a specific meaning (`Role does not exist` → run `SetupAssumeRole`); don't collapse it into a generic 500. |
| Caching | The four caches (`markerCache`, `assumeRoleCache`, `listAccountsCache`, `accountIDCache`) are configured under `osis.scality.vault.cache.*`. Changes to TTLs, capacity, or cache keys need a reason — `assumeRoleCache` TTL is **50 minutes** because the session token is valid for 60. Going above 60 will break flows. |
| Secret handling | Secret keys are encrypted via `osis.security.keys.cipher` (32-byte) and stored in Redis Sentinel hash `osis:s3credentials` keyed `<Username>__<AccessKeyID>`. Any change to the encryption path, key format, or hash schema is a data-migration concern. Never log secrets, access keys, or session tokens. |
| AWS SDK usage | Region must come from config (`osis.scality.region`), not hardcoded. Credentials providers must use the `assumeRoleCache` for user-scoped calls, never super-admin creds for user-facing flows. STS session token refresh must use AWS SDK `refresh()` — don't roll your own. |
| Dependency pinning | `vaultclientVersion`, `springBootVersion`, AWS SDK versions live in root `build.gradle`. Bumping any of these requires a coverage check and a Sonatype publish step; flag if the bump is unexplained. |
| `application.properties` | The in-tree file is a **template**. Real values mount at `/conf/application.properties` in the container. Flag any commit that bakes in a non-default endpoint, real access key, or production hostname into the template. |
| Test coverage | JaCoCo enforces **75% minimum** via `jacocoTestCoverageVerification`. New service/controller code without a corresponding test will likely drop coverage and fail `check`. Exclusion list in `jacoco.gradle:60-67` — don't expand it without a strong reason. |
| Logging | Use SLF4J `Logger logger = LoggerFactory.getLogger(...)`. No `System.out.println`, no `printStackTrace()` in production code. Match log level to severity; `DEBUG` is fine, `ERROR` should mean operator-actionable. |
| Health endpoints | Health is served at `/_/healthcheck`, not the Spring default. New health indicators belong in `osis-app/.../healthcheck/` and must extend `HealthIndicator`. Utapi healthcheck was intentionally removed in 2.2.5 — flag any attempt to re-add it without justification. |
| Vendored crypto | `security/crypto/Hkdf*` is vendored from an external library and explicitly excluded from coverage and lint. Don't refactor it as if it were ours; flag any modification. |
| OSE REST contract | OSE drives this service. Changes to request/response models in `osis-core/.../model/` (e.g. `OsisTenant`, `OsisUser`, `OsisS3Credential`, `Page*`) are potentially breaking for OSE consumers. Flag field renames, removals, or required-vs-optional flips. |
| Security | OWASP-relevant: hard-coded credentials, SQL/log injection (no string-concatenated SQL or log args), path traversal in any file-read of `crypto.yml` or admin creds, secrets logged in error paths. |

4. **Deliver your review:**

### If CI mode: post to GitHub

#### Part A: Inline file comments

For each issue, post a comment on the exact file and line. Keep comments short (1-3 sentences), end with `— Claude Code`. Use line numbers from the **new version** of the file.

**Without suggestion block** — single-line command, `<br>` for line breaks:
```bash
gh api -X POST -H "Accept: application/vnd.github+json" "repos/<owner/repo>/pulls/<number>/comments" -f body="Issue description.<br><br>— Claude Code" -f path="file" -F line=42 -f side="RIGHT" -f commit_id="<headRefOid>"
```

**With suggestion block** — use a heredoc (`-F body=@-`) so code renders correctly:
```bash
gh api -X POST -H "Accept: application/vnd.github+json" "repos/<owner/repo>/pulls/<number>/comments" -F body=@- -f path="file" -F line=42 -f side="RIGHT" -f commit_id="<headRefOid>" <<'COMMENT_BODY'
Issue description.

```suggestion
first line of suggested code
second line of suggested code
```

— Claude Code
COMMENT_BODY
```

Only suggest when you can show the exact replacement. For architectural or design issues, just describe the problem.

#### Part B: Summary comment

Single-line command, `<br>` for line breaks. No markdown headings — they render as giant bold text. Flat bullet list only:

```bash
gh pr comment <number> --repo <owner/repo> --body "- file:line — issue<br>- file:line — issue<br><br>Review by Claude Code"
```

If no issues: just say "LGTM". End with: `Review by Claude Code`

### If local mode: output the review as text

Do NOT post anything to GitHub. Instead, output the review directly as text.

For each issue found, output:

```
**<file_path>:<line_number>** — <what's wrong and how to fix it>
```

When the fix is a concrete line change, include a fenced code block showing the suggested replacement.

At the end, output a summary section listing all issues. If no issues: just say "LGTM".

End with: `Review by Claude Code`

## What NOT to do

- Do not comment on markdown formatting preferences
- Do not suggest refactors unrelated to the PR's purpose
- Do not praise code — only flag problems or stay silent
- If no issues are found, post only a summary saying "LGTM"
- Do not flag style issues already covered by the project's linter (SpotBugs, PMD) — but **do** flag if a change adds something that those linters would catch *if* the `com/scality/osis/**` PMD exclusion were lifted (since today's PMD coverage on Scality code is effectively zero per `pmd.gradle:20`)
