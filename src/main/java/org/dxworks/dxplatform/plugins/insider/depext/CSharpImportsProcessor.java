package org.dxworks.dxplatform.plugins.insider.depext;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;

import java.util.Collections;
import java.util.List;

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
        return trimmedLine.startsWith("namespace") ?
                trimmedLine.substring(10).trim().replaceAll(";", "") : null;
    }


    @Override
    protected List<ImportItem> importLine(String trimmedLine) {
        if (trimmedLine.startsWith("using") == false) return null;

        if (trimmedLine.contains("(")) return null;

        if (trimmedLine.startsWith("using static ")) {
            return Collections.singletonList(new ImportItem(trimmedLine.substring(13).trim().replaceAll(";", ""), "static"));
        }

        return Collections.singletonList(new ImportItem(trimmedLine.substring(6).trim().replaceAll(";", "")));
    }
}
