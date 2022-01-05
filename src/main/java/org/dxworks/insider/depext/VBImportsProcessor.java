package org.dxworks.insider.depext;

import org.dxworks.insider.InsiderFile;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class VBImportsProcessor extends AbstractImportsProcessor {
    public static int counter = 0;

    public VBImportsProcessor(InsiderFile insiderFile) {
        super(insiderFile);
        counter++;
    }

    @Override
    protected String language() {
        return "visualbasic";
    }

    @Override
    protected String namespaceLine(String trimmedLine) {
        if (trimmedLine.trim().startsWith("Namespace") == false) return null;
        Pattern p = Pattern.compile("\\s*Namespace\\s([\\w.]+)(.*)");
        Matcher m = p.matcher(trimmedLine.trim());
        return m.find() ? m.group(1) : null;
    }

    protected List<ImportItem> importLine(String trimmedLine) {
        Pattern p = Pattern.compile("Imports\\s([\\w.]+\\s*=*\\s*[\\w.]+)(.*)");
        Matcher m = p.matcher(trimmedLine.trim());
        if (m.find() == false) return null;

        String[] importedEntity = m.group(1).split("=");

        if (importedEntity.length == 1) return Collections.singletonList(new ImportItem(importedEntity[0].trim()));
        return Collections.singletonList(new ImportItem(importedEntity[1].trim()));
    }
}
