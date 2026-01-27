package org.dxworks.insider.depext;

import org.dxworks.insider.InsiderFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RustImportsProcessor extends AbstractImportsProcessor {
    public static int counter = 0;
    private static final Pattern CRATE_NAME_PATTERN = Pattern.compile("^\\s*name\\s*=\\s*\"([^\"]+)\"");

    public RustImportsProcessor(InsiderFile insiderFile) {
        super(insiderFile);
        
        counter++;
        
        String relativePath = insiderFile.getFullyQualifiedName();
        if (!isUnderSrcDirectory(relativePath)) {
            namespace = "";
            return;
        }
        
        try {
            String crateName = findCrateNameFromPath(insiderFile.getPath());
            namespace = calculateNamespace(relativePath, crateName);
        } catch (Exception e) {
            namespace = "";
        }
    }

    public static String statistics() {
        return counter + " Rust files processed";
    }

    @Override
    protected String language() {
        return "rust";
    }

    @Override
    protected String namespaceLine(String trimmedLine) {
        // Namespace is calculated in constructor from file path, not from source code
        return null;
    }

    @Override
    protected List<ImportItem> importLine(String trimmedLine) {
        String cleanedLine = trimmedLine.replaceAll("/\\*[#\\s]*\\*/", "").trim();
        
        if (!cleanedLine.matches("^(pub\\s+)?use\\s+.*")) return null;
        
        String attribute = "";
        String line = cleanedLine;
        
        if (line.matches("^pub\\s+use\\s+.*")) {
            attribute = "pub";
            line = line.replaceFirst("^pub\\s+use\\s+", "");
        } else {
            line = line.replaceFirst("^use\\s+", "");
        }
        
        line = line.replaceAll(";$", "").trim();
        
        if (line.contains("*")) {
            attribute = attribute.isEmpty() ? "glob" : attribute + ",glob";
        }
        
        return Collections.singletonList(new ImportItem(line, attribute));
    }

    private boolean isUnderSrcDirectory(String filePath) {
        String normalizedPath = filePath.replace("\\", "/");
        return normalizedPath.contains("/src/");
    }

    private String findCrateNameFromPath(String filePath) throws IOException {
        Path rootFolder = Paths.get(org.dxworks.insider.configuration.InsiderConfiguration.getInstance().getRootFolder()).toAbsolutePath();
        Path path = Paths.get(filePath).toAbsolutePath();
        Path currentDir = path.getParent();
        
        while (currentDir != null) {
            Path cargoTomlPath = currentDir.resolve("Cargo.toml");
            if (Files.exists(cargoTomlPath)) {
                String crateName = extractCrateNameFromCargoToml(cargoTomlPath);
                if (crateName != null) {
                    return crateName.replace("-", "_");
                }
            }
            
            // Stop if we've reached the root folder
            if (currentDir.equals(rootFolder)) {
                break;
            }
            
            currentDir = currentDir.getParent();
        }
        
        throw new IOException("Could not find Cargo.toml with [package] section");
    }

    private String extractCrateNameFromCargoToml(Path cargoTomlPath) throws IOException {
        List<String> lines = Files.readAllLines(cargoTomlPath);
        boolean inPackageSection = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.equals("[package]")) {
                inPackageSection = true;
                continue;
            }
            
            if (trimmed.startsWith("[") && !trimmed.equals("[package]")) {
                inPackageSection = false;
                continue;
            }
            
            if (inPackageSection) {
                Matcher matcher = CRATE_NAME_PATTERN.matcher(trimmed);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        
        return null;
    }

    private String calculateNamespace(String filePath, String crateName) {
        String normalizedPath = filePath.replace("\\", "/");
        
        int srcIndex = normalizedPath.lastIndexOf("/src/");
        if (srcIndex == -1) {
            return crateName;
        }
        
        String relativePath = normalizedPath.substring(srcIndex + 5);
        
        if (relativePath.equals("lib.rs") || relativePath.equals("main.rs")) {
            return crateName;
        }
        
        relativePath = relativePath.replaceAll("\\.rs$", "");
        
        if (relativePath.endsWith("/mod")) {
            relativePath = relativePath.substring(0, relativePath.length() - 4);
        }
        
        String modulePathPart = relativePath.replace("/", "::");
        
        if (modulePathPart.isEmpty()) {
            return crateName;
        }
        
        return crateName + "::" + modulePathPart;
    }
}
