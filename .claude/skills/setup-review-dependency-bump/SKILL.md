---
name: setup-review-dependency-bump
description: Add the review-dependency-bump CI workflow to a repo, integrating with existing review.yml if present
---

# Setup Dependency Bump Review Workflow

Add the `review-dependency-bump` CI job to a repository so dependabot PRs are automatically evaluated.

## Steps

### 0. Resolve the latest workflow version

Run:

```bash
gh release list --repo scality/workflows --limit 1 --json tagName --jq '.[0].tagName'
```

Use the returned tag (e.g. `v2.7.0`) in all `scality/workflows` references below, replacing `LATEST_TAG` in the templates.

### 1. Check existing setup

Look for `.github/workflows/review.yml` and any other workflow files that reference `dependabot` or `dependency-review` or `review-dependency-bump`.

Determine which scenario applies:

- **A) Dependency review already configured** — a workflow already calls `claude-code-dependency-review.yml` or already has a `review-dependency-bump` job. Tell the user it's already set up, then update the existing job definition in place with the latest template from step 2a (job block only) or 2b so that new secrets, env vars, or parameters are picked up. Show the diff of what changed.
- **B) `review.yml` exists with a code review job but no dependency review** — add the dependency-bump job to the existing workflow (step 2a).
- **C) No `review.yml` exists** — create a new one with only the dependency-bump job (step 2b).

### 2a. Add dependency-bump job to existing `review.yml`

Read the existing `review.yml`. Make three changes:

**First**, add `pull_request_target` to the `on:` triggers alongside the existing `pull_request` trigger. Keep the existing `pull_request` trigger exactly as-is (preserve its types, paths, branches filters). The new `pull_request_target` trigger only needs `[opened, synchronize]`:

```yaml
on:
  pull_request:
    # ... keep existing types/filters unchanged ...
  pull_request_target:
    types: [opened, synchronize]
```

**Second**, guard the existing review job so it only runs for non-dependabot PRs on the `pull_request` event:

```yaml
  review:
    # May also trigger on dependabot branches when a human updates them (acceptable double-review).
    if: github.event_name == 'pull_request' && github.actor != 'dependabot[bot]'
```

If the existing job already has an `if:` condition, combine them with `&&`.

**Third**, append the dependency-bump job at the end:

```yaml
  review-dependency-bump:
    # pr.user.login catches dependabot PRs updated by a human, where github.actor is no longer dependabot[bot].
    if: github.event_name == 'pull_request_target' && (github.actor == 'dependabot[bot]' || github.event.pull_request.user.login == 'dependabot[bot]')
    uses: scality/workflows/.github/workflows/claude-code-dependency-review.yml@LATEST_TAG
    with:
      ACTIONS_APP_ID: ${{ vars.ACTIONS_APP_ID }}
    secrets:
      GCP_WORKLOAD_IDENTITY_PROVIDER: ${{ secrets.GCP_WORKLOAD_IDENTITY_PROVIDER }}
      GCP_SERVICE_ACCOUNT: ${{ secrets.GCP_SERVICE_ACCOUNT }}
      ANTHROPIC_VERTEX_PROJECT_ID: ${{ secrets.ANTHROPIC_VERTEX_PROJECT_ID }}
      CLOUD_ML_REGION: ${{ secrets.CLOUD_ML_REGION }}
      ACTIONS_APP_PRIVATE_KEY: ${{ secrets.ACTIONS_APP_PRIVATE_KEY }}
```

If there is a **separate** workflow file (e.g. `review-dependency-bump.yml`) that handles dependency reviews with a different approach (inline action steps instead of the reusable workflow), stop and ask the user whether to delete it and consolidate into `review.yml`, or leave it as-is.

### 2b. Create new `review.yml`

Create `.github/workflows/review.yml`:

```yaml
name: Code Review

on:
  pull_request_target:
    types: [opened, synchronize]

jobs:
  review-dependency-bump:
    # pr.user.login catches dependabot PRs updated by a human, where github.actor is no longer dependabot[bot].
    if: github.actor == 'dependabot[bot]' || github.event.pull_request.user.login == 'dependabot[bot]'
    uses: scality/workflows/.github/workflows/claude-code-dependency-review.yml@LATEST_TAG
    with:
      ACTIONS_APP_ID: ${{ vars.ACTIONS_APP_ID }}
    secrets:
      GCP_WORKLOAD_IDENTITY_PROVIDER: ${{ secrets.GCP_WORKLOAD_IDENTITY_PROVIDER }}
      GCP_SERVICE_ACCOUNT: ${{ secrets.GCP_SERVICE_ACCOUNT }}
      ANTHROPIC_VERTEX_PROJECT_ID: ${{ secrets.ANTHROPIC_VERTEX_PROJECT_ID }}
      CLOUD_ML_REGION: ${{ secrets.CLOUD_ML_REGION }}
      ACTIONS_APP_PRIVATE_KEY: ${{ secrets.ACTIONS_APP_PRIVATE_KEY }}
```

### 3. Summary

Tell the user what was generated/modified and what they need to do next:

- Ensure their repo has access to the organization-level secrets `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT`, `ANTHROPIC_VERTEX_PROJECT_ID`, `CLOUD_ML_REGION`, and `ACTIONS_APP_PRIVATE_KEY` (grant access via the org's **Settings > Secrets and variables > Actions**, or ask an org admin)
- Ensure the organization variable `ACTIONS_APP_ID` is available to the repo (same settings page, under **Variables**)
- Commit and push the changes
