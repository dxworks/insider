# AGENTS.md

> Project conventions and context for AI coding agents.

## Project Overview

**Insider** is a regex-based code analysis tool by DXWorks. It detects code smells, library usage, and software topics using only regular expressions — no AST parsing — making it language-independent. Built with Java 21 and Gradle 9, distributed as a JAR, npm package (`@dxworks/insider`), and Docker image.

## Build & Run

```bash
# Build (includes tests)
gradle clean build

# Build without tests
gradle clean build -x test

# Run tests
gradle test

# Run
java -Xmx4g -jar build/libs/insider-*.jar <command> [options]
```

## Project Structure

```
src/main/java/org/dxworks/insider/   # Main source code
  ├── Insider.java                   # Entry point (main class)
  ├── commands/                      # CLI commands (Command pattern)
  ├── depext/                        # Dependency extraction (per-language processors)
  ├── technology/finder/             # Library/technology detection via regex
  ├── application/inspector/         # Rule-based code inspection
  ├── configuration/                 # Config management (singleton)
  ├── library/detector/              # Library detection
  ├── constants/                     # Constants
  └── utils/                         # Utilities
src/test/                            # JUnit 4 tests
config/                              # Runtime config (fingerprints, rules, comments, ignore patterns)
lib/                                 # Node.js CLI wrapper
bin/                                 # Shell/batch execution scripts
docs/                                # MkDocs documentation
releaseNotes/                        # Per-version release notes
scripts/                             # Automation/regression scripts
```

## Key Conventions

- **Java 21** — use modern Java features where appropriate.
- **Lombok** — used extensively (`@Slf4j`, `@Getter`, `@Data`, etc.). Do not write boilerplate that Lombok handles.
- **Command pattern** — all operations implement `InsiderCommand`. Three variants: `NoFilesCommand` (no file input), `AllFilesCommand` (all files), and regular commands (filtered by extension).
- **Configuration** — singleton via `InsiderConfiguration.getInstance()`. Supports properties file (`config/insider-conf.properties`) and environment variables (`INSIDER_*`).
- **Logging** — SLF4J with Logback. Use `@Slf4j` annotation.
- **CLI framework** — Picocli for argument parsing.
- **Testing** — JUnit 4 (`@Test`, `Assert.*`). Tests live in `src/test/java/` mirroring the main source tree.
- **Dependency locking** — Gradle lock file is committed (`gradle.lockfile`). Run `gradle dependencies --write-locks` after changing dependencies.

## Dependencies

Managed in `build.gradle`. Key libraries: Jackson (JSON/YAML), OpenCSV, Commons IO/Lang3/Collections4, Picocli, Logback. DXWorks internal libs: `dx-ignore`, `argumenthor`, `dx-linguist`.

## CI/CD

GitHub Actions workflows in `.github/workflows/`. Most use reusable workflows from [`dxworks/pipelines`](https://github.com/dxworks/pipelines).

### PR workflows (on PRs to `dev`)
- **build.yml** — builds on every push
- **regression-test.yml** — compares output against latest release
- **trivy-security-scan.yml** — Trivy filesystem scan (reusable) + Docker image build & scan (inline)

### Release workflows (on tag push)
- **release.yml** — triggered by `v*` tags (excluding `*-voyager`). Composable pipeline:
  `parse-tag → gate → archive / npm / docker (parallel) → github-release`
- **release-voyager.yml** — triggered by `v*-voyager` tags. Same pattern but only: `gate → archive → github-release` (no npm/Docker)

### Scheduled workflows
- **trivy-daily-scan.yml** — daily Trivy scan of dependencies + Docker Hub image (`dxworks/insider:latest`)

### Release scripts (convention-based)
- `scripts/build.sh` — build the project (called by reusable workflows)
- `scripts/prepare-release.sh` — package release assets into ZIP
- `scripts/prepare-release-voyager.sh` — same but includes `instrument.yml`

### Reusable workflows (`dxworks/pipelines@v1`)
- `release-gate.yml` — quality gate (build + test + Trivy scan)
- `release-archive.yml` — build and package release archive
- `release-npm.yml` — publish to GitHub Packages + npmjs.org (OIDC Trusted Publishers)
- `release-docker.yml` — multi-arch Docker build (amd64 + arm64) with Trivy scan gate
- `release-github.yml` — create GitHub Release with auto-generated notes
- `trivy-fs-scan.yml`, `trivy-image-scan.yml`, `trivy-daily-scan.yml` — security scanning

## Branching

- Main branch: **dev**
- Releases are tagged `v*` (e.g., `v2.13.0`)
- Voyager releases are tagged `v*-voyager` (e.g., `v2.13.0-voyager`)

## Output

Results are written to a `results/` directory as JSON/CSV files, named with the project ID prefix.
