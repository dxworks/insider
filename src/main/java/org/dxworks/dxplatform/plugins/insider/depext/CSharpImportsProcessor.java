package org.dxworks.dxplatform.plugins.insider.depext;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSharpImportsProcessor extends AbstractImportsProcessor {
    public static int counter = 0;

    public CSharpImportsProcessor(InsiderFile insiderFile) {
        super(insiderFile);
        counter++;
    }

    @Override
    protected String language() {
        return "csharp";
    }

    @Override
    protected String namespaceLine(String trimmedLine) {
        return trimmedLine.startsWith("namespace") && trimmedLine.length() >= 10 ?
            trimmedLine.substring(10).trim().replaceAll(";", "") : null;
    }


    @Override
    protected List<ImportItem> importLine(String trimmedLine) {
        if (trimmedLine.startsWith("using") == false) return null;
        if (trimmedLine.contains("(")) return null;

        Pattern p = Pattern.compile("(using|using static)\\s+([\\w.]+\\s*)=*\\s*([\\w.<>\\s,]*);");
        Matcher m = p.matcher(trimmedLine.trim());
        if (m.find() == false) return null;

        String attribute = "";
        String importedNamespace = "";

        if (trimmedLine.startsWith("using static")) {
            attribute = "static";
            importedNamespace = m.group(3);
        } else {
            importedNamespace = trimmedLine.contains("=") ? m.group(3) : m.group(2);
        }

        importedNamespace = importedNamespace.replace(",", "");

        return Collections.singletonList(new ImportItem(importedNamespace, attribute));
    }
}
