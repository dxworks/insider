package org.dxworks.insider.depext;

import org.dxworks.insider.InsiderFile;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CPPImportsProcessor extends AbstractImportsProcessor {
    public static int counter = 0;

    public CPPImportsProcessor(InsiderFile insiderFile) {
        super(insiderFile);
        counter++;
    }

    @Override
    protected String language() {
        return "cpp";
    }

    @Override
    protected String namespaceLine(String trimmedLine) {
        if (trimmedLine.startsWith("namespace")) {
            String patternString = "(namespace\\s+)(\\w+)(.*)";

            Matcher matcher = Pattern.compile(patternString).matcher(trimmedLine);
            if (matcher.find()) return matcher.group(2);
        }

        return null;
    }


    @Override
    protected List<ImportItem> importLine(String trimmedLine) {
        if (trimmedLine.startsWith("#include ") == false) return null;

        String patternString = "(#include\\s+<)([\\w\\d/.\\\\]+)(>.*)";

        Matcher matcher = Pattern.compile(patternString).matcher(trimmedLine);
        if (matcher.find())
            return Collections.singletonList(new ImportItem(matcher.group(2), "library"));

        patternString = "(#include\\s+\")([\\w/.]+)(\")";

        matcher = Pattern.compile(patternString).matcher(trimmedLine);
        if (matcher.find())
            return Collections.singletonList(new ImportItem(matcher.group(2)));

        return null;
    }

}
