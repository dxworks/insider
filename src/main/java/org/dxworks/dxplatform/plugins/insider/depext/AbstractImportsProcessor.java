package org.dxworks.dxplatform.plugins.insider.depext;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.utils.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractImportsProcessor {
    protected List<String> lines;
    private final InsiderFile file;
    protected String namespace;
    private final List<ImportItem> importedItems = new ArrayList<>();

    public AbstractImportsProcessor(InsiderFile file) {

        lines = Arrays.asList(FileUtils.removeComments(file.getContent()).split("\n"));
        this.file = file;

        lines.stream()
            .map(String::trim)
            .map(it -> it.replaceAll("[^\\x00-\\x7F]", ""))
            .forEach(trimmedLine -> {
                if (namespace == null) {
                    String foundNamespace = namespaceLine(trimmedLine);
                    if (foundNamespace != null) namespace = foundNamespace;
                }
                List<ImportItem> crtImportItems = importLine(trimmedLine);
                if (crtImportItems != null) importedItems.addAll(crtImportItems);
            });

    }

    public ImportResult extract() {
        ImportResult resultObject = new ImportResult();

        resultObject.filename = file.getFullyQualifiedName();
        resultObject.language = language();
        resultObject.namespace = namespace();
        resultObject.importedItems = importedItems;
        resultObject.linesOfCode = (lines != null) ? lines.size() : 0;

        return resultObject;
    }

    private String namespace() {
        return namespace == null ? "" : namespace;
    }

    protected abstract String language();

    protected abstract String namespaceLine(String trimmedLine);

    protected abstract List<ImportItem> importLine(String trimmedLine);

}
