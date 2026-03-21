package org.dxworks.insider.depext;

import org.dxworks.insider.configuration.InsiderConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the crate name and root directory for a Rust source file.
 * 
 * <h2>Algorithm</h2>
 * <pre>
 * 1. Start from the file's directory
 * 2. Walk up the directory tree looking for Cargo.toml
 * 3. When found, check if it has a [package] section with name = "..."
 * 4. If yes, that's our crate. Convert hyphens to underscores in the name.
 * 5. Stop at the project root folder to avoid escaping the workspace.
 * </pre>
 * 
 * <h2>Why hyphens → underscores?</h2>
 * Cargo allows crate names like "my-crate", but Rust identifiers can't have hyphens.
 * In code, you write: use my_crate::module;
 */
public class RustCrateResolver {

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\s*name\\s*=\\s*\"([^\"]+)\"");

    public static class CrateInfo {
        private final String name;
        private final Path root;
        
        public CrateInfo(String name, Path root) {
            this.name = name;
            this.root = root;
        }
        
        public String getName() { return name; }
        public Path getRoot() { return root; }
    }

    public CrateInfo resolve(String absoluteFilePath) throws IOException {
        Path projectRoot = Paths.get(InsiderConfiguration.getInstance().getRootFolder()).toAbsolutePath();
        Path currentDir = Paths.get(absoluteFilePath).toAbsolutePath().getParent();
        
        while (currentDir != null) {
            Path cargoToml = currentDir.resolve("Cargo.toml");
            
            if (Files.exists(cargoToml)) {
                String name = findCrateNameInCargoToml(cargoToml);
                if (name != null) {
                    return new CrateInfo(name.replace("-", "_"), currentDir);
                }
            }
            
            // Don't search above project root
            if (currentDir.equals(projectRoot)) {
                break;
            }
            currentDir = currentDir.getParent();
        }
        
        throw new IOException("No Cargo.toml with [package] section found for: " + absoluteFilePath);
    }

    /**
     * Parse Cargo.toml to find: [package] ... name = "crate-name"
     */
    private String findCrateNameInCargoToml(Path cargoToml) throws IOException {
        List<String> lines = Files.readAllLines(cargoToml);
        boolean inPackage = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Track which TOML section we're in
            if (trimmed.equals("[package]")) {
                inPackage = true;
            } else if (trimmed.startsWith("[")) {
                inPackage = false;
            }
            
            // Look for name = "..." in [package] section
            if (inPackage) {
                Matcher m = NAME_PATTERN.matcher(trimmed);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        
        return null;
    }
}
