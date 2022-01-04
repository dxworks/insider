package org.dxworks.dxplatform.plugins.insider.depext;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JavastackImportsProcessor extends AbstractImportsProcessor {
    public static int javaCounter = 0;
    public static int kotlinCounter = 0;
    public static int scalaCounter = 0;
    public static int groovyCounter = 0;

    private final String language;

    public JavastackImportsProcessor(InsiderFile insiderFile, String language) {
        super(insiderFile);

        this.language = language;

        switch (this.language) {
            case "java":
                javaCounter++;
                break;
            case "scala":
                scalaCounter++;
                break;
            case "kotlin":
                kotlinCounter++;
                break;
            case "groovy":
                groovyCounter++;
                break;
        }
    }

    public static boolean hasFiles() {
        return javaCounter + kotlinCounter + scalaCounter + groovyCounter > 0;
    }

    public static String statistics() {
        List<String> rows = new ArrayList<>();
        if (javaCounter > 0) rows.add(javaCounter + " Java files processed");
        if (kotlinCounter > 0) rows.add(kotlinCounter + " Kotlin files processed");
        if (groovyCounter > 0) rows.add(groovyCounter + " Groovy files processed");
        if (scalaCounter > 0) rows.add(scalaCounter + " Scala files processed");

        return String.join("\n", rows);
    }

    @Override
    protected String namespaceLine(String trimmedLine) {
        return trimmedLine.startsWith("package") ? trimmedLine.substring(8).trim().replaceAll(";", "") : null;
    }

    @Override
    protected String language() {
        return language;
    }

    @Override
    protected List<ImportItem> importLine(String trimmedLine) {
        if (!trimmedLine.startsWith("import ")) return null;

        if (trimmedLine.contains("(")) return null;
        if (trimmedLine.contains("=>")) return null;

        String attribute = "";
        if (trimmedLine.startsWith("import static ")) {
            trimmedLine = trimmedLine.substring(14);
            attribute = "static";
        } else trimmedLine = trimmedLine.substring(7);

        trimmedLine = trimmedLine.trim().replaceAll(";", "").replace("}", "").replace("`", "").replace("._", "");

        String[] parts = trimmedLine.split("\\{");
        if (parts.length == 1) return Collections.singletonList(new ImportItem(trimmedLine, attribute));

        String prefix = parts[0];

        String[] listOfImports = parts[1].split(",");

        List<ImportItem> importItems = new ArrayList<>();
        for (String importItem : listOfImports) new ImportItem((prefix + importItem.trim()), attribute);
        return importItems;
    }
}
