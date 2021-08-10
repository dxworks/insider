package org.dxworks.dxplatform.plugins.insider.application.inspector.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.math.IntRange;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.InsiderResult;
import org.dxworks.dxplatform.plugins.insider.technology.finder.LinguistService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Rule {
    private String name;
    private String id;
    private String description;
    private List<String> applies_to;
    private List<String> tags;
    private String severity;
    private List<InsiderPattern> patterns;
    private List<String> overrides;
    private List<Condition> conditions;
    private String _comment;


    public List<InsiderResult> analyze(InsiderFile file, List<IntRange> commentRanges) {
        if (appliesTo(file)) {
            List<PatternMatch> matches = patterns.parallelStream()
                    .flatMap(pattern -> pattern.getMatches(file, commentRanges).stream())
                    .collect(Collectors.toList());

            if (matches.size() > 0)
                return tags.stream()
                        .map(tag -> InsiderResult.builder()
                                .file(file.getFullyQualifiedName())
                                .category("")
                                .name(tag)
                                .value(matches.size())
                                .build())
                        .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public boolean appliesTo(InsiderFile insiderFile) {
        return CollectionUtils.isEmpty(applies_to) || LinguistService.getInstance().hasAcceptedExtension(insiderFile, applies_to);
    }

    public void transformPatterns() {
        patterns.forEach(InsiderPattern::transformPattern);
    }
}
