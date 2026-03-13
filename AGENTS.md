# AGENTS Guide for `insider`

This file is for coding agents working in this repository.
It documents how to build, test, lint, and follow project conventions.

## Project Snapshot

- Language: Java (primary), with a small Node wrapper/package step.
- Build tool: Gradle Wrapper (`./gradlew`) with Gradle 9 wrapper.
- Java compatibility for code: Java 11 (`sourceCompatibility`/`targetCompatibility`).
- Java runtime required by Gradle 9: Java 17+ to run the wrapper.
- Test framework: JUnit 4 (`org.junit.Test`, `org.junit.Assert`).
- Packaging: Gradle builds JAR, npm scripts assemble distribution assets.

## Repository Rules Discovery

- Cursor rules: none found (`.cursor/rules/` and `.cursorrules` not present).
- Copilot rules: none found (`.github/copilot-instructions.md` not present).
- If these files are added later, treat them as higher-priority guidance than this file.

## Environment Setup

1. Use Java 17+ to run Gradle wrapper commands.
2. Use Node 18+ for npm packaging flows (matches CI release workflow).
3. Prefer wrapper commands over system Gradle (`./gradlew ...`).
4. Keep working directory at repository root when running commands below.

## Build Commands

### Main build

- Linux/macOS: `./gradlew clean build`
- Windows: `gradlew.bat clean build`

This compiles Java sources, runs tests, and creates the runnable JAR in `build/libs/`.

### Fast compile-only loops

- `./gradlew clean classes`
- `./gradlew testClasses`

Use these when you only need compile feedback.

### Package npm distribution assets

- `npm ci`
- `npm run build`

`npm run build` expects a built JAR at `build/libs/insider*.jar` and creates `dist/`.

## Test Commands

### Run all tests

- Linux/macOS: `./gradlew test`
- Windows: `gradlew.bat test`

### Run a single test class (important)

- `./gradlew test --tests "org.dxworks.insider.utils.FileUtilsTest"`

### Run a single test method (important)

- `./gradlew test --tests "org.dxworks.insider.utils.FileUtilsTest.removeComments"`
- `./gradlew test --tests "org.dxworks.insider.depext.RustImportsProcessorTest.testLibRsNamespace"`

### Run tests in one package

- `./gradlew test --tests "org.dxworks.insider.depext.*"`

### Useful test debugging flags

- `./gradlew test --tests "..." --info`
- `./gradlew test --tests "..." --stacktrace`

## Lint / Static Analysis

There is no dedicated Checkstyle/Spotless/PMD configuration in this repository.

Use these as practical "lint" gates:

- `./gradlew check` (includes test lifecycle checks)
- `./gradlew build` (full validation used in CI)

If introducing a new lint tool, keep it minimal and consistent with existing style.

## Documentation Commands

- Local docs preview (if `mkdocs` installed): `mkdocs serve`
- Build docs: `mkdocs build`

CI docs release workflow uses `mkdocs gh-deploy --force` from branch `docs`.

## Code Style Guidelines

### Formatting and structure

- Use 4 spaces for indentation; do not use tabs.
- Keep braces on the same line for class/method/control declarations.
- Keep one public top-level class/interface per file.
- Use short logical spacing blocks; avoid excessive blank lines.
- Prefer readable streams; avoid deeply nested lambda chains when plain loops are clearer.

### Imports

- Order imports by groups: third-party, then `org.dxworks...`, then `java...`, with blank lines between groups.
- Put static imports after normal imports (current test style follows this).
- Prefer explicit imports; avoid new wildcard imports unless there is a strong local precedent.
- Remove unused imports.

### Types and APIs

- Target Java 11 language features for source code compatibility.
- Prefer interfaces in signatures (`List`, `Map`) and concrete types in constructors.
- Use generics explicitly; avoid raw types.
- Avoid introducing `var` unless it improves readability clearly.
- Keep DTOs/simple models concise; Lombok (`@Data`, `@NoArgsConstructor`, etc.) is accepted in this codebase.

### Naming conventions

- Classes/interfaces: `PascalCase`.
- Methods/fields/local variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Package names: all lowercase (`org.dxworks.insider...`).
- Test methods: descriptive `camelCase`, usually behavior-focused.

### Error handling and logging

- Prefer logging with context for recoverable failures (`log.error`, `log.warn`).
- Preserve causes when rethrowing (`new InsiderException(message, cause)`).
- Fail fast on invalid CLI input; print usage/help as current commands do.
- Do not silently swallow exceptions unless the flow intentionally degrades gracefully.
- When graceful fallback is used, return stable defaults (for example, empty namespace/string) and continue.

### CLI and command behavior

- Keep command parsing strict and deterministic.
- Validate file/folder inputs before execution (`fileExists`, `folderExists` patterns).
- Keep output paths consistent with `results/` and project configuration conventions.
- For user-facing errors, keep messages actionable and concise.

### File/path handling

- Use `Path`/`Paths`/`Files` from `java.nio.file` for filesystem operations.
- Normalize separators to `/` when producing cross-platform relative paths.
- Avoid hardcoded absolute paths.

### Concurrency and performance

- Existing code uses `parallelStream()` in hot paths; preserve thread-safety when editing those flows.
- Avoid shared mutable state in parallel sections unless synchronized.
- Keep memory usage in mind when reading full file content.

### Tests

- Follow JUnit 4 style annotations (`@Test`, `@Before`, `@Rule`).
- Use `TemporaryFolder` for filesystem isolation when needed.
- Prefer focused assertions with clear expected values.
- Add regression tests for parsing/path edge cases when fixing bugs.

## Change Management for Agents

- Keep changes minimal and localized to the requested behavior.
- Do not refactor unrelated modules in the same patch.
- Update tests alongside behavior changes.
- Run at least targeted tests for edited areas; run full `./gradlew test` when practical.
- If build tooling fails because of Java runtime mismatch, report it and provide the exact required JDK version.

## CI Alignment

- CI build workflow executes: `gradle clean build`.
- Release workflows also run Gradle build and npm packaging.
- Before proposing release-impacting changes, verify both Java build and npm packaging paths still work.
