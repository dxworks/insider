package org.dxworks.insider.depext;

import org.dxworks.insider.InsiderFile;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Processes Rust source files to extract namespace and import information.
 * 
 * This class orchestrates three focused components:
 * 1. {@link RustContentPreprocessor} - Prepares content (string neutralization, multi-line joining)
 * 2. {@link RustCrateResolver} - Finds crate name and root from Cargo.toml
 * 3. {@link RustNamespaceResolver} - Calculates namespace from file path and #[path] attributes
 * 
 * Import parsing (use statements) is handled directly here as it's simple line-by-line logic.
 */
public class RustImportsProcessor extends AbstractImportsProcessor {
    
    public static int counter = 0;
    
    private static final Pattern USE_STATEMENT_PATTERN = Pattern.compile("^(pub\\s+)?use\\s+.*");
    
    private static final RustContentPreprocessor contentPreprocessor = new RustContentPreprocessor();
    private static final RustCrateResolver crateResolver = new RustCrateResolver();
    private static final RustNamespaceResolver namespaceResolver = new RustNamespaceResolver();

    public RustImportsProcessor(InsiderFile insiderFile) {
        super(contentPreprocessor.preprocess(insiderFile));
        counter++;
        namespace = resolveNamespace(insiderFile);
    }
    
    private String resolveNamespace(InsiderFile insiderFile) {
        String relativePath = insiderFile.getFullyQualifiedName();
        String absolutePath = insiderFile.getPath();
        
        if (!namespaceResolver.isUnderSrcDirectory(relativePath)) {
            return "";
        }
        
        try {
            RustCrateResolver.CrateInfo crateInfo = crateResolver.resolve(absolutePath);
            return namespaceResolver.resolve(absolutePath, relativePath, crateInfo.getName(), crateInfo.getRoot());
        } catch (Exception e) {
            return "";
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
        String cleanedLine = removeInlineComments(trimmedLine);
        
        if (!isUseStatement(cleanedLine)) {
            return null;
        }
        
        boolean isPublic = cleanedLine.startsWith("pub ");
        String importPath = extractImportPath(cleanedLine);
        String attribute = buildAttribute(isPublic, importPath);
        
        return Collections.singletonList(new ImportItem(importPath, attribute));
    }
    
    private String removeInlineComments(String line) {
        return line.replaceAll("/\\*[#\\s]*\\*/", "").trim();
    }
    
    private boolean isUseStatement(String line) {
        return USE_STATEMENT_PATTERN.matcher(line).matches();
    }
    
    private String extractImportPath(String line) {
        String withoutKeywords = line.replaceFirst("^pub\\s+use\\s+", "")
                                     .replaceFirst("^use\\s+", "");
        return withoutKeywords.replaceAll(";$", "").trim();
    }
    
    private String buildAttribute(boolean isPublic, String importPath) {
        boolean isGlob = importPath.contains("*");
        
        if (isPublic && isGlob) {
            return "pub,glob";
        } else if (isPublic) {
            return "pub";
        } else if (isGlob) {
            return "glob";
        }
        return "";
    }
}
