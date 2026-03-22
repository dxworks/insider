# Repository Modernization Playbook

## Task List

1. **Build tool upgrade** — Upgrade Gradle/Maven/Node to latest versions
2. **Language version upgrade** — Upgrade Java/Node runtime to target version (e.g., Java 21)
3. **Dependency updates** — Upgrade all library dependencies, fix deprecations, migrate abandoned libs
4. **GitHub Actions modernization** — Update all actions to latest versions, fix missing steps
5. **Dependency lockfile generation** — Generate lockfiles for vulnerability scanning (Gradle, npm)
6. **Vulnerability scan & fix** — Run Trivy, fix all findings to zero
7. **Dockerfile modernization** — Update base images, patch OS-level vulnerabilities
8. **Trivy CI workflow** — Add security scanning using reusable workflows from `dxworks/pipelines`
9. **Release pipeline modernization** — Replace monolithic release with composable reusable workflows
10. **Regression test suite** — Compare current build output against latest release
11. **AGENTS.md generation** — Create tool-agnostic project context file

---

## Detailed Agent Instructions

### 1. Build tool upgrade

```
Upgrade the build tool to the latest stable version:
- Gradle: run `./gradlew wrapper --gradle-version <latest>`, verify build passes
- Maven: update maven-wrapper.properties to latest version
- Node: update engines field in package.json if present
Do NOT change any dependencies or language versions — only the build tool itself.
```

### 2. Language version upgrade

```
Upgrade the project's language/runtime version to Java 21 (or specified target):
- build.gradle/pom.xml: update toolchain, sourceCompatibility, targetCompatibility
- GitHub Actions workflows: update setup-java to match new version
- Fix any compilation errors caused by the upgrade
- Verify with a clean build
```

### 3. Dependency updates

```
Upgrade all project dependencies to their latest stable versions:
- Gradle/Maven: bump all library versions in build files
- npm: run npm update, fix any breaking changes
- If a dependency is abandoned/archived, migrate to its maintained replacement
  (e.g., commons-lang 2.x -> commons-lang3, update all import statements and API calls)
- Verify with a clean build and existing tests
```

### 4. GitHub Actions modernization

```
Update all GitHub Actions workflows (.github/workflows/*.yml):
- Bump all action versions to latest (e.g., actions/checkout@v4, actions/setup-java@v4,
  actions/upload-artifact@v4, etc.)
- Change Java distribution to 'temurin' (replace 'adopt', 'zulu', etc.)
- Add missing setup steps (e.g., if a job runs Gradle but has no setup-java step, add one)
- Ensure Java/Node versions in workflows match the project's target version
- Do NOT change workflow logic or add new workflows
```

### 5. Dependency lockfile generation

```
Generate dependency lockfiles so Trivy can scan resolved dependencies:
- Gradle: add `dependencyLocking { lockAllConfigurations() }` to build.gradle,
  run `./gradlew dependencies --write-locks`
- npm: ensure package-lock.json exists and is up to date
- Maven: ensure pom.xml has resolved versions (no ranges)
- Ensure gradle/wrapper/ is tracked in git (fix .gitignore if needed)
- Commit the lockfiles
```

### 6. Vulnerability scan & fix

```
Run `trivy fs .` and fix ALL vulnerabilities:
- For each finding, upgrade the dependency to a patched version
- If no patched version exists, find an alternative library and migrate
- For npm: use "overrides" in package.json for transitive dependency fixes
- Re-run `trivy fs .` after fixes — target is ZERO vulnerabilities
- Verify the build still passes after all changes
```

### 7. Dockerfile modernization

```
Update the project's Dockerfile:
- Replace the base image with a current, minimal image matching the project's
  runtime version (e.g., eclipse-temurin:21-jre-alpine for Java 21)
- Add `RUN apk upgrade --no-cache` (Alpine) or equivalent to patch OS vulnerabilities
- Verify the image builds: `docker build -t test .`
- Run `trivy image test` — target is ZERO vulnerabilities
- Do NOT change the application logic, just the base image and OS packages
```

### 8. Trivy CI workflow

Uses reusable workflows from [`dxworks/pipelines`](https://github.com/dxworks/pipelines).

```
Create .github/workflows/trivy-security-scan.yml:
- Trigger: on PRs to the main development branch
- Job 1 — Filesystem scan (reusable):
  uses: dxworks/pipelines/.github/workflows/trivy-fs-scan.yml@v1
  - Scans dependencies, uploads SARIF to GitHub Security tab
  - Fails the check if MEDIUM+ vulnerabilities found
- Job 2 — Docker image scan (reusable or inline):
  Option A (pre-built image): uses: dxworks/pipelines/.github/workflows/trivy-image-scan.yml@v1
    with: image-ref, build-context, build-setup, java-version, post-pr-comment
  Option B (inline): build the image locally, scan with trivy-action, upload SARIF
  - Supports PR comments with CVE table and link to Security tab
- Inputs available: severity, fail-on-findings, post-pr-comment

Create .github/workflows/trivy-daily-scan.yml:
- Trigger: schedule (cron '0 6 * * *') + workflow_dispatch
  uses: dxworks/pipelines/.github/workflows/trivy-daily-scan.yml@v1
  with: image-ref (Docker Hub image, e.g. dxworks/myapp:latest)
  - Scans dependencies + published Docker image daily
  - Uploads to GitHub Security tab for alerting
  - Does not fail — reporting only

Required permissions: contents: read, security-events: write, pull-requests: write (for PR comments)
Required GitHub setting: GitHub Advanced Security enabled (free for public repos)
```

### 9. Release pipeline modernization

Uses composable reusable workflows from [`dxworks/pipelines`](https://github.com/dxworks/pipelines).
Each repo picks only the steps it needs.

```
Dependency graph:
  parse-tag → gate → archive / npm / docker (parallel) → github-release

Create .github/workflows/release.yml:
- Trigger: on push tags 'v*' (exclude '*-voyager')
- Job: parse-tag — extract semver from tag (e.g. v2.13.0 → 2.13.0)
- Job: gate — quality gate (build + test + Trivy fs scan)
  uses: dxworks/pipelines/.github/workflows/release-gate.yml@v1
  with: java-version, node-version, build-script (default: ./scripts/build.sh)
- Job: archive — build and package release ZIP
  uses: dxworks/pipelines/.github/workflows/release-archive.yml@v1
  with: version, java-version, build-script, prepare-script (default: ./scripts/prepare-release.sh)
  Convention: each repo has scripts/build.sh and scripts/prepare-release.sh
- Job: npm (optional) — publish to GitHub Packages + npmjs.org
  uses: dxworks/pipelines/.github/workflows/release-npm.yml@v1
  with: version
  Uses OIDC Trusted Publishers — no NPM_TOKEN secret needed
  Requires: id-token: write permission
  One-time setup: configure Trusted Publisher on npmjs.org (repo + workflow)
  Auto-detects prerelease versions (e.g. 2.13.0-rc.1) and uses --tag <prerelease-id>
- Job: docker (optional) — multi-arch build + push
  uses: dxworks/pipelines/.github/workflows/release-docker.yml@v1
  with: version, image-name, platforms (default: linux/amd64,linux/arm64),
        push-dockerhub, push-ghcr, build-script
  secrets: dockerhub-username, dockerhub-token
  Includes Trivy scan: blocks on CRITICAL, warns on MEDIUM/HIGH
- Job: github-release — create GitHub Release (runs last)
  uses: dxworks/pipelines/.github/workflows/release-github.yml@v1
  with: version, tag, release-name, asset-name, release-notes-file
  Uses gh CLI with --generate-notes for auto-generated release notes
  Idempotent: re-runs upload assets to existing release

Create .github/workflows/release-voyager.yml (if applicable):
- Trigger: on push tags 'v*-voyager'
- Same building blocks but different composition (e.g. no npm/docker)
- Uses scripts/prepare-release-voyager.sh for different packaging

All publish steps are idempotent — safe to re-run after partial failures.
```

### 10. Regression test suite

```
Create scripts/regression-test.sh:
- Download the latest released binary via `gh release download`
- Build the current branch (Gradle shadowJar / Maven package / npm build)
- Clone 2 representative test repos
- Run both binaries against each test repo with the same config
- Compare all output files (ignore logs and timestamps)
- Generate a JUnit XML report: each repo = test suite, each output file = test case,
  include diff in failure messages

Create .github/workflows/regression-test.yml:
- Trigger: workflow_dispatch + PRs (non-blocking / continue-on-error)
- Run the script
- Upload report artifact
- Render results with dorny/test-reporter
```

### 11. AGENTS.md generation

```
Create AGENTS.md at the repo root (tool-agnostic, not Claude-specific):
- Explore the full project: source tree, build files, CI, config, scripts, commands
- Include sections: Project Overview, Build & Run, Project Structure,
  Key Conventions, Dependencies, CI/CD, Branching Strategy, Output Format
- Keep it factual and derived from the actual code — no speculation
- Target audience: any AI coding agent (Claude, Copilot, Cursor, Cline, Aider)
```

---

## Execution Order

```
Phase 1 (sequential): Tasks 1 → 2 → 3 → 5 (build tool, language, deps, lockfiles)
Phase 2 (parallel):   Tasks 4, 6, 7 (actions, vuln fix, Dockerfile)
Phase 3 (parallel):   Tasks 8, 9, 10, 11 (CI workflows, release pipeline, regression tests, AGENTS.md)
```

Tasks in Phase 1 must run sequentially because each depends on the previous.
Phase 2 can start after Phase 1 completes. Phase 3 can start after Phase 2.

## Prerequisites

- [`dxworks/pipelines`](https://github.com/dxworks/pipelines) repo must be public and tagged `@v1`
- GitHub Advanced Security enabled on target repos (free for public repos)
- For npm OIDC: configure Trusted Publishers on npmjs.org per package
- For Docker Hub: org-level secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN`
