package org.dxworks.dxplatform.plugins.insider.technology.finder.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dxworks.dxplatform.plugins.insider.InsiderAnalysis;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.InsiderResult;
import org.dxworks.dxplatform.plugins.insider.technology.finder.LinguistService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class Technology implements InsiderAnalysis {

    private final LinguistService linguistService = LinguistService.getInstance();
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
    public InsiderResult analyze(InsiderFile insiderFile) {
        if (!accepts(insiderFile))
            return null;

        int totalOccurrences = patterns.parallelStream()
                .mapToInt(pattern -> getPatternOccurrencesInFile(insiderFile, pattern))
                .sum();

        return InsiderResult.builder()
                .file(insiderFile.getFullyQualifiedName())
                .category(category)
                .name(name)
                .value(totalOccurrences)
                .build();
    }

    @Override
    public boolean accepts(InsiderFile insiderFile) {
        return linguistService.hasAcceptedExtension(insiderFile.getPath(), languages)
                || extensions.contains(insiderFile.getExtension());
    }

    private int getPatternOccurrencesInFile(InsiderFile insiderFile, Pattern pattern) {
        int fileOcc = 0;
        Matcher matcher = pattern.matcher(insiderFile.getContent());

        while (matcher.find())
            fileOcc++;
        return fileOcc;
    }

    public void setFingerprints(List<String> fingerprints) {
        this.fingerprints = fingerprints;
        patterns = fingerprints.stream().map(Pattern::compile).collect(Collectors.toList());
    }
}
