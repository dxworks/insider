package org.dxworks.dxplatform.plugins.insider.technology.finder.model;

import lombok.Builder;
import lombok.Data;
import org.dxworks.dxplatform.plugins.insider.InsiderAnalysis;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.InsiderResult;
import org.dxworks.dxplatform.plugins.insider.technology.finder.LanguageRegistry;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Builder
public class Technology implements InsiderAnalysis {

    private final LanguageRegistry languageRegistry = LanguageRegistry.getInstance();
    private String category;
    private String name;
    private List<String> languages;
    private List<String> extensions;
    private List<String> fingerprints;
    private List<Pattern> patterns;

    public Technology(String category, String name, List<String> languages, List<String> extensions,
                      List<String> fingerprints) {
        this.category = category;
        this.name = name;
        this.languages = languages;
        this.extensions = extensions;
        this.fingerprints = fingerprints;
        patterns = fingerprints.stream().map(Pattern::compile).collect(Collectors.toList());
    }

    @Override
    public List<InsiderResult> analyze(InsiderFile insiderFile) {
        if (!accepts(insiderFile.getExtension()))
            return Collections.emptyList();

        return patterns.parallelStream()
                .map(pattern -> InsiderResult.builder()
                        .file(insiderFile.getPath())
                        .category(category)
                        .name(name)
                        .value(getPatternOccurrencesInFile(insiderFile, pattern))
                        .build())
                .collect(Collectors.toList());
    }

    private int getPatternOccurrencesInFile(InsiderFile insiderFile, Pattern pattern) {
        int fileOcc = 0;
        Matcher matcher = pattern.matcher(insiderFile.getContent());

        while (matcher.find())
            fileOcc++;
        return fileOcc;
    }

    private boolean accepts(String extension) {
        return languages.stream().anyMatch(language -> languageRegistry.isOfLanguage(language, extension))
                || extensions.contains(extension);
    }
}
