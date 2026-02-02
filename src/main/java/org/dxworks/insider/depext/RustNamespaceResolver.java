package org.dxworks.insider.depext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the namespace (module path) for a Rust source file.
 * 
 * <h2>Algorithm</h2>
 * <pre>
 * 1. If file is lib.rs or main.rs in src/ → namespace = crate_name
 * 2. Otherwise → namespace = crate_name::module_path
 * 
 * Module path is normally derived from physical directory structure:
 *   src/network/tcp.rs → network::tcp
 *   src/network/mod.rs → network
 * 
 * Exception: #[path] attributes can override the physical-to-logical mapping.
 * </pre>
 * 
 * <h2>#[path] Attribute Handling</h2>
 * <pre>
 * In Rust, a parent module can declare:
 *   #[path = "custom/location.rs"]
 *   mod logical_name;
 * 
 * This means the file at "custom/location.rs" has logical name "logical_name",
 * not "custom::location" as the physical path would suggest.
 * 
 * We check for such declarations in:
 *   1. Crate root files (lib.rs, main.rs)
 *   2. Parent module files (mod.rs or parent_dir.rs)
 * </pre>
 */
public class RustNamespaceResolver {

    private static final Pattern PATH_ATTR = Pattern.compile("#\\[\\s*path\\s*=\\s*\"([^\"]+)\"\\s*\\]");
    private static final Pattern MOD_DECL = Pattern.compile("^\\s*(?:pub\\s+)?mod\\s+(\\w+)\\s*;");

    public boolean isUnderSrcDirectory(String relativePath) {
        String normalized = relativePath.replace("\\", "/").toLowerCase();
        return normalized.contains("/src/") || normalized.startsWith("src/");
    }

    /**
     * Main entry point: resolve full namespace for a Rust file.
     */
    public String resolve(String absolutePath, String relativePath, String crateName, Path crateRoot) throws IOException {
        String pathAfterSrc = extractPathAfterSrc(relativePath);
        
        if (pathAfterSrc == null) {
            return crateName;
        }
        
        if (pathAfterSrc.equalsIgnoreCase("lib.rs") || pathAfterSrc.equalsIgnoreCase("main.rs")) {
            return crateName;
        }
        
        Path srcDir = crateRoot.resolve("src");
        Path targetFile = Paths.get(absolutePath);
        String modulePath = resolveModulePath(srcDir, targetFile);
        
        return crateName + "::" + modulePath;
    }

    /**
     * Resolve the module path (the part after crate_name::).
     * 
     * Strategy:
     * 1. Try to find a #[path] override in parent modules
     * 2. If not found, use physical directory structure
     */
    private String resolveModulePath(Path srcDir, Path targetFile) throws IOException {
        // First, try to find a #[path] attribute that overrides this file's logical name
        String overriddenPath = findPathAttributeOverride(srcDir, targetFile);
        if (overriddenPath != null) {
            return overriddenPath;
        }
        
        // No override found, derive from physical path
        return physicalPathToModulePath(srcDir, targetFile);
    }

    /**
     * Convert physical path to module path using standard Rust conventions.
     * 
     * Examples:
     *   src/network.rs      → network
     *   src/network/mod.rs  → network
     *   src/network/tcp.rs  → network::tcp
     */
    String physicalPathToModulePath(Path srcDir, Path targetFile) {
        Path relative = srcDir.relativize(targetFile);
        List<String> segments = new ArrayList<>();
        
        // Add all directory names as module segments
        for (int i = 0; i < relative.getNameCount() - 1; i++) {
            segments.add(relative.getName(i).toString());
        }
        
        // Add filename as segment (unless it's mod.rs, which represents the parent dir)
        String fileName = relative.getFileName().toString();
        if (!fileName.equalsIgnoreCase("mod.rs")) {
            segments.add(fileName.replace(".rs", ""));
        }
        
        return String.join("::", segments);
    }

    /**
     * Search for a #[path] attribute in parent modules that overrides this file's logical name.
     * 
     * We check these locations (in order):
     * 1. Crate root: src/lib.rs, src/main.rs
     * 2. Each parent directory's module file: mod.rs or parent_name.rs
     */
    private String findPathAttributeOverride(Path srcDir, Path targetFile) throws IOException {
        Path relative = srcDir.relativize(targetFile);
        
        // Check crate root files
        String result = checkCrateRootForPathAttribute(srcDir, targetFile);
        if (result != null) {
            return result;
        }
        
        // For files deeper than src/foo.rs, check parent module files
        if (relative.getNameCount() > 1) {
            return checkParentModulesForPathAttribute(srcDir, targetFile, relative);
        }
        
        return null;
    }

    /**
     * Check lib.rs and main.rs for #[path] declarations pointing to targetFile.
     */
    private String checkCrateRootForPathAttribute(Path srcDir, Path targetFile) throws IOException {
        for (String rootFile : new String[]{"lib.rs", "main.rs"}) {
            Path rootPath = findCaseInsensitiveFile(srcDir, rootFile);
            if (rootPath != null) {
                String logicalName = findPathDeclarationFor(rootPath, targetFile);
                if (logicalName != null) {
                    return logicalName;
                }
            }
        }
        return null;
    }

    /**
     * Find a file in a directory, ignoring case.
     * Returns null if not found.
     */
    private Path findCaseInsensitiveFile(Path dir, String fileName) throws IOException {
        // First try exact match (most common case)
        Path exactPath = dir.resolve(fileName);
        if (Files.exists(exactPath)) {
            return exactPath;
        }
        
        // Use Files.walk() with case-insensitive filter - more efficient than listing all files
        try (var stream = Files.walk(dir, 1)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equalsIgnoreCase(fileName))
                .findFirst()
                .orElse(null);
        }
    }

    /**
     * Check parent module files for #[path] declarations.
     * 
     * For a file at src/network/custom/module/mod.rs, we check:
     *   - src/network/mod.rs for #[path] pointing to custom/module/mod.rs
     *   - src/network/custom/mod.rs for #[path] pointing to module/mod.rs
     *   - etc.
     * 
     * If src/network/mod.rs has "#[path = custom/module/mod.rs] mod module;",
     * we return "network::module" (prefix "network" + logical name "module").
     */
    private String checkParentModulesForPathAttribute(Path srcDir, Path targetFile, Path relative) throws IOException {
        // For src/network/custom/module/mod.rs:
        //   depth=1: check src/network/mod.rs, prefix=""
        //   depth=2: check src/network/custom/mod.rs, prefix="network"
        //   depth=3: check src/network/custom/module/mod.rs, prefix="network::custom"
        
        int segmentCount = relative.getNameCount();
        
        for (int depth = 1; depth < segmentCount; depth++) {
            // parentDir is the directory containing the module file we'll check
            Path parentDir = srcDir;
            for (int i = 0; i < depth; i++) {
                parentDir = parentDir.resolve(relative.getName(i).toString());
            }
            
            // prefix is the logical path up to (but not including) this module
            List<String> prefixSegments = new ArrayList<>();
            for (int i = 0; i < depth; i++) {
                prefixSegments.add(relative.getName(i).toString());
            }
            
            // Check mod.rs in parentDir (case-insensitive)
            Path modRs = findCaseInsensitiveFile(parentDir, "mod.rs");
            if (modRs != null) {
                String logicalName = findPathDeclarationFor(modRs, targetFile);
                if (logicalName != null) {
                    return joinWithPrefix(prefixSegments, logicalName);
                }
            }
            
            // Check sibling .rs file (e.g., src/network.rs instead of src/network/mod.rs)
            String dirName = relative.getName(depth - 1).toString();
            Path siblingRs = parentDir.getParent().resolve(dirName + ".rs");
            if (Files.exists(siblingRs)) {
                String logicalName = findPathDeclarationFor(siblingRs, targetFile);
                if (logicalName != null) {
                    // For sibling .rs, prefix excludes the current dir name
                    List<String> siblingPrefix = new ArrayList<>();
                    for (int i = 0; i < depth - 1; i++) {
                        siblingPrefix.add(relative.getName(i).toString());
                    }
                    return joinWithPrefix(siblingPrefix, logicalName);
                }
            }
        }
        
        return null;
    }

    private String joinWithPrefix(List<String> prefix, String name) {
        if (prefix.isEmpty()) {
            return name;
        }
        return String.join("::", prefix) + "::" + name;
    }

    /**
     * Search a module file for a #[path] declaration that points to targetFile.
     * 
     * Looks for pattern:
     *   #[path = "some/path.rs"]
     *   mod logical_name;
     * 
     * If the resolved path matches targetFile, returns "logical_name".
     */
    private String findPathDeclarationFor(Path moduleFile, Path targetFile) throws IOException {
        List<String> lines = Files.readAllLines(moduleFile);
        Path moduleDir = moduleFile.getParent();
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            // Look for #[path = "..."]
            Matcher pathMatcher = PATH_ATTR.matcher(line);
            if (!pathMatcher.find()) {
                continue;
            }
            
            String declaredPathStr = pathMatcher.group(1);
            Path declaredPath = moduleDir.resolve(declaredPathStr).normalize();
            
            // Check if this path points to our target file
            if (!pathPointsToFile(declaredPath, targetFile)) {
                continue;
            }
            
            // Found a match! Now extract the logical module name from "mod xxx;"
            String modName = extractModNameFromLine(line);
            if (modName == null && i + 1 < lines.size()) {
                modName = extractModNameFromLine(lines.get(i + 1).trim());
            }
            
            if (modName != null) {
                return modName;
            }
        }
        
        return null;
    }

    /**
     * Check if declaredPath points to targetFile.
     * Either directly, or declaredPath is a directory containing targetFile.
     */
    private boolean pathPointsToFile(Path declaredPath, Path targetFile) {
        // Direct file match
        try {
            if (Files.exists(declaredPath) && Files.isSameFile(declaredPath, targetFile)) {
                return true;
            }
        } catch (IOException e) {
            // ignore
        }
        
        // Check if declaredPath is a parent directory of targetFile
        // (for #[path = "dir/mod.rs"] where targetFile is dir/submodule.rs)
        try {
            Path declaredDir = declaredPath.getParent();
            if (declaredDir != null && targetFile.startsWith(declaredDir)) {
                Path relativeToDir = declaredDir.relativize(targetFile);
                // The first segment should match the declared file's name (without extension)
                String declaredFileName = declaredPath.getFileName().toString().replace(".rs", "").replace("/mod", "");
                String firstSegment = relativeToDir.getName(0).toString().replace(".rs", "");
                return firstSegment.equals(declaredFileName) || relativeToDir.getName(0).equals(declaredPath.getFileName());
            }
        } catch (Exception e) {
            // ignore path resolution errors
        }
        
        return false;
    }

    private String extractModNameFromLine(String line) {
        Matcher m = MOD_DECL.matcher(line);
        return m.find() ? m.group(1) : null;
    }

    private String extractPathAfterSrc(String relativePath) {
        String normalized = relativePath.replace("\\", "/").toLowerCase();
        
        int srcIdx = normalized.lastIndexOf("/src/");
        if (srcIdx != -1) {
            return relativePath.substring(relativePath.toLowerCase().indexOf("/src/") + 5);
        }
        
        if (normalized.startsWith("src/")) {
            return relativePath.substring(relativePath.toLowerCase().indexOf("src/") + 4);
        }
        
        return null;
    }
}
