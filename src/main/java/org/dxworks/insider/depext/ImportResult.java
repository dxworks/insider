package org.dxworks.insider.depext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImportResult {
    String filename;
    String language;
    String namespace;
    Integer linesOfCode;

    List<ImportItem> importedItems;

    public String toString() {
        String escapedFilename = CsvUtils.escapeCsvValue(filename);
        String escapedLanguage = CsvUtils.escapeCsvValue(language);
        String escapedNamespace = CsvUtils.escapeCsvValue(namespace);
        
        if (importedItems.isEmpty())
            return escapedFilename + "," + escapedLanguage + "," + escapedNamespace + "," + linesOfCode + ",";

        List<String> importLines = importedItems.stream().map(ImportItem::toString).collect(Collectors.toList());

        String fileColumns = escapedFilename + "," + escapedLanguage + "," + escapedNamespace + "," + linesOfCode;

        List<String> rows = new ArrayList<>();

        importLines.forEach(importItem -> rows.add(fileColumns + "," + importItem));
        return String.join("\n", rows);
    }

    public static String header() {
        return "file,language,namespace,#lines,import,import_attribute";
    }
}
