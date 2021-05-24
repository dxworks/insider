package org.dxworks.dxplatform.plugins.insider.depext;

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
        if (importedItems.isEmpty())
            return filename + "," + language + "," + namespace + "," + linesOfCode + ",";

        List<String> importLines = importedItems.stream().map(ImportItem::toString).collect(Collectors.toList());

        String fileColumns = filename + "," + language + "," + namespace + "," + linesOfCode;

        List<String> rows = new ArrayList<>();

        importLines.forEach(importItem -> rows.add(fileColumns + "," + importItem));
        return String.join("\n", rows);
    }

    public static String header() {
        return "file,language,namespace,#lines,import,import_attribute";
    }
}
