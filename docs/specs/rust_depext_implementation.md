# Rust Dependency Extractor Implementation

## Problem Statement

We need to extend the Insider dependency extractor to support Rust projects. The extractor analyzes source files to extract:
- **Namespace/module** for each file
- **Import/dependency statements** (`use` statements in Rust)
- Output results to CSV format for component-level structural analysis

### Challenges Specific to Rust

1. **Module System Complexity**
   - Rust uses `mod` declarations to define module structure, not traditional package declarations
   - Module hierarchy is defined by both file structure AND `mod` declarations in parent files
   - Each file's namespace depends on its position in the module tree

2. **Multi-Crate Workspaces**
   - A Rust project can contain multiple crates (sub-packages)
   - Each crate has its own `Cargo.toml` and module tree
   - `use crate::module` refers to different modules depending on which crate the file belongs to
   - Example workspace structure:
     ```
     workspace-root/
     ├── Cargo.toml              # Workspace root
     ├── src/lib.rs              # Main crate "main-lib"
     └── crates/
         ├── network-lib/
         │   ├── Cargo.toml      # Crate "network-lib"
         │   └── src/lib.rs
         └── utils-lib/
             ├── Cargo.toml      # Crate "utils-lib"
             └── src/lib.rs
     ```

3. **Crate Naming**
   - Crate names in `Cargo.toml` can use hyphens: `name = "example-crate"`
   - In code, hyphens are converted to underscores: `use example_crate::module`
   - This is a Rust language requirement (identifiers cannot contain hyphens)

4. **Import Syntax Variations**
   - Single import: `use std::collections::HashMap;`
   - Multiple imports: `use std::io::{Read, Write};`
   - Glob imports: `use std::fs::*;`
   - Relative imports: `use super::parent;`, `use self::child;`
   - Re-exports: `pub use tcp::Connection;`
   - Internal references: `use crate::network::tcp;`
   - External references: `use other_crate::network::tcp;`

## Context

### Existing Architecture

The depext system uses a template method pattern:

1. **`AbstractImportsProcessor`** - Base class that:
   - Removes comments from file content
   - Iterates through each line
   - Calls abstract methods `namespaceLine()` and `importLine()`
   - Collects namespace (first match) and all import items
   - Returns `ImportResult` with: filename, language, namespace, imports, LOC

2. **Language-specific processors** (e.g., `JavastackImportsProcessor`, `CSharpImportsProcessor`) extend the base and implement:
   - `language()` - returns language identifier
   - `namespaceLine(String)` - parses namespace/package declaration
   - `importLine(String)` - parses import statements

3. **`DependencyExtractor`** orchestrates:
   - Routes files to appropriate processor based on extension
   - Collects all results
   - Writes to CSV: `results/{projectID}-depext.csv`

### CSV Output Format

```
file,language,namespace,#lines,import,import_attribute
```

Each file produces multiple rows (one per import), with file metadata repeated.

### Design Constraints

- **No post-processing**: The tool runs on client infrastructure without debugging access
- **Minimal complexity**: Keep processing simple and deterministic
- **No cross-file analysis**: Each file is processed independently
- **No output format changes**: Must maintain existing CSV structure

## Solution

### Approach: File-Path-Based Namespace Inference

We will implement a **pragmatic, minimal approach** that:
1. **Only processes `.rs` files under `src/` directories** (skips benchmarks, tests, examples)
2. Uses file paths to infer module structure (assumes standard Rust conventions)
3. Finds the crate root and reads crate name from `Cargo.toml`
4. Calculates namespace based on file path relative to `{crate_root}/src/`
5. Parses `use` statements and keeps them as-is in the output
6. Does NOT parse `mod` declarations (assumes file structure matches module structure)

**Assumptions**: 
- Module names match file/directory names (true ~95% of the time in idiomatic Rust)
- Only library/binary source code under `src/` is analyzed (production code focus)

### Namespace Calculation Algorithm

For each `.rs` file **under a `src/` directory**:

1. **Verify file is under `src/`**: Skip files not under a `src/` directory (e.g., `benches/`, `tests/`, `examples/`)
2. **Find crate root**: Walk up directory tree until finding `Cargo.toml` with `[package]` section
3. **Extract crate name**: Parse `[package] name = "crate-name"` from `Cargo.toml`
4. **Convert crate name**: Replace hyphens with underscores (`example-crate` → `example_crate`)
5. **Calculate relative path**: Get file path relative to `{crate_root}/src/`
6. **Build namespace**:
   - `src/lib.rs` or `src/main.rs` → `{crate_name}`
   - `src/module.rs` → `{crate_name}::module`
   - `src/network/mod.rs` → `{crate_name}::network`
   - `src/network/tcp.rs` → `{crate_name}::network::tcp`

**Note**: This algorithm correctly handles multi-crate workspaces:
- Files in `crates/network-lib/src/session.rs` → finds `crates/network-lib/Cargo.toml` → namespace `network_lib::session`
- Files in `xtask/src/main.rs` → finds `xtask/Cargo.toml` → namespace `xtask`

### Import Statement Handling

Parse `use` statements and extract as `ImportItem`:

| Rust Statement | ImportItem(name, attribute) |
|----------------|----------------------------|
| `use std::collections::HashMap;` | `("std::collections::HashMap", "")` |
| `use std::io::{Read, Write};` | `("std::io::{Read, Write}", "")` |
| `use std::fs::*;` | `("std::fs::*", "glob")` |
| `pub use tcp::Connection;` | `("tcp::Connection", "pub")` |
| `use crate::network::tcp;` | `("crate::network::tcp", "")` |
| `use super::utils;` | `("super::utils", "")` |

**Key decisions**:
- Keep curly brace syntax as-is (e.g., `{Read, Write}`) - let aggregation layer expand if needed
- Track `pub` attribute for re-exports (important for API boundaries)
- Track `glob` attribute for wildcard imports (affects dependency precision)
- Keep relative paths (`crate::`, `super::`, `self::`) as-is

**Attribute semantics** (aligned with existing languages):
- `""` (empty): Regular imports - default case
- `"pub"`: Re-exports that affect public API surface (similar to `static` in Java/C#)
- `"glob"`: Wildcard imports representing unknown dependencies (similar to C++ library/local distinction)

### Aggregation Layer Responsibilities

The aggregation layer (outside this tool) will:
1. Normalize `use crate::module` by replacing `crate::` with the actual crate name from the file's namespace
2. Expand curly brace imports if needed: `std::io::{Read, Write}` → two separate dependencies
3. Match import statements to file namespaces to build structural relations
4. Handle `pub use` re-exports for component API analysis

## Implementation Plan

### Phase 1: Create RustImportsProcessor
1. Create `src/main/java/org/dxworks/insider/depext/RustImportsProcessor.java`
2. Extend `AbstractImportsProcessor`
3. Implement constructor:
   - Accept `InsiderFile` parameter
   - **Verify file path contains `/src/` directory** - if not, skip processing (set namespace to empty)
   - Find crate root by walking up directory tree to find `Cargo.toml` with `[package]` section
   - Parse `Cargo.toml` to extract crate name
   - Convert hyphens to underscores in crate name
   - Calculate namespace from file path relative to `{crate_root}/src/`
   - Call `super(insiderFile)` to initialize base class
4. Implement `language()`: return `"rust"`
5. Implement `namespaceLine()`: return `null` (namespace set in constructor)
6. Implement `importLine()`:
   - If file was skipped (not under `src/`), return `null` for all lines
   - Detect `use` statements
   - Parse full import path (including `{}` syntax)
   - Extract attributes: `pub`, `glob` (for `*`)
   - Return `List<ImportItem>` with single item (no expansion)

### Phase 2: Integration
1. Update `DependencyExtractor.java`:
   - Add `.rs` extension to accepted file types
   - Add routing logic to create `RustImportsProcessor` for `.rs` files
   - Add counter for Rust files processed
2. Test with sample Rust projects

### Phase 3: Testing & Validation
1. Test with single-crate Rust project
2. Test with multi-crate workspace
3. Verify namespace calculation for various file locations
4. Verify import parsing for all syntax variants
5. Check CSV output format compatibility

## Known Limitations

### Files Not Under `src/` Directory
Files outside `src/` directories (e.g., `/benches/*.rs`, `/tests/*.rs`, `/examples/*.rs`) are processed differently:
- **Namespace**: Set to empty string `""` (cannot determine module structure without `src/` context)
- **Imports**: Still extracted normally (dependency information is valuable)
- **Use case**: Allows tracking dependencies in test/benchmark/example code

This includes:
1. **Benchmark files** (`/benches/*.rs`)
2. **Integration tests** (`/tests/*.rs`)
3. **Example programs** (`/examples/*.rs`)
4. **Fuzzing targets** (`/fuzz/**/*.rs`)
5. **Build scripts** (`build.rs` not under `src/`)

Files under `src/` directories get full namespace calculation:
- Main library/binary source code (`/src/`)
- Workspace member crates (`/crates/*/src/`)
- Build tool crates (`/xtask/src/`)

### Processing Limitations
1. **Module name mismatches**: If a `mod` declaration uses a different name than the file/directory, the namespace will be incorrect
2. **Conditional compilation**: `#[cfg(...)]` attributes on `mod` declarations are ignored
3. **Inline modules**: `mod inline_module { ... }` in the same file are not detected
4. **Path attributes**: `#[path = "custom/path.rs"]` on `mod` declarations are not handled

These limitations are acceptable given the constraint of minimal complexity and the focus on production code structure.

## Future Enhancements

If higher accuracy is needed:
1. Parse `mod` declarations to build accurate module tree (requires two-pass processing)
2. Handle `#[path]` attributes for non-standard file locations
3. Detect and handle inline modules
4. Support for `#[cfg]` conditional compilation
