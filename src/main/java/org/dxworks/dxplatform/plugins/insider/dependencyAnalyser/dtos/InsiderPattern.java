package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.math.IntRange;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InsiderPattern {
    private static final String REGEX_WORD = "regex-word";

    private String pattern;
    private String type;
    private String confidence;
    private List<String> modifiers;
    private List<String> scopes;
    private String _comment;
    @JsonIgnore
    private int regexFlags = -1;

    public List<PatternMatch> getMatches(InsiderFile file, List<IntRange> commentRanges) {
        if (scopes.contains("all") || scopes.containsAll(Arrays.asList("code", "comment"))) {
            return matchesInEntireFile(file);
        }

        List<PatternMatch> matches = new ArrayList<>();

        if (scopes.contains("code")) {
            matches.addAll(getCodeMatches(file, commentRanges));
        }
        if (scopes.contains("comment")) {
            matches.addAll(getCommentMatches(file, commentRanges));
        }

        return matches;
    }

    public void transformPattern() {
        if (REGEX_WORD.equals(type))
            transformPatternIfWord();
        if ("string".equals(type)) {
            pattern = Pattern.quote(pattern);
            pattern = String.format("\\b(%s)\\b", pattern);
        }
        if ("substring".equals(type))
            pattern = Pattern.quote(pattern);
    }

    private List<PatternMatch> matchesInEntireFile(InsiderFile file) {
        Matcher matcher = Pattern.compile(pattern, createModifier()).matcher(file.getContent());

        List<PatternMatch> matches = new ArrayList<>();
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            PatternMatch patternMatch = createPatternMatch(start, end, file);
            matches.add(patternMatch);
        }
        return matches;
    }

    private int createModifier() {
        if(regexFlags != -1)
            return regexFlags;

        regexFlags = 0 ;

        if(modifiers.contains("i"))
            regexFlags = regexFlags | CASE_INSENSITIVE;

        if(modifiers.contains("d"))
            regexFlags = regexFlags | DOTALL;

        if(modifiers.contains("m"))
            regexFlags = regexFlags | MULTILINE;

        return regexFlags;
    }

    private List<PatternMatch> getCommentMatches(InsiderFile file, List<IntRange> commentRanges) {
        return matchesInEntireFile(file).stream()
                .filter(patternMatch -> isInComment(patternMatch, commentRanges))
                .collect(Collectors.toList());
    }

    private boolean isInComment(PatternMatch patternMatch, List<IntRange> commentRanges) {
        return commentRanges.stream().anyMatch(range -> range.containsRange(patternMatch.getAbsoluteRange()));
    }

    private boolean isOutsideOfComment(PatternMatch patternMatch, List<IntRange> commentRanges) {
        return commentRanges.stream().noneMatch(range -> range.overlapsRange(patternMatch.getAbsoluteRange()));
    }

    private List<PatternMatch> getCodeMatches(InsiderFile file, List<IntRange> commentRanges) {
        return matchesInEntireFile(file).stream()
                .filter(patternMatch -> isOutsideOfComment(patternMatch, commentRanges))
                .collect(Collectors.toList());
    }

    private PatternMatch createPatternMatch(int start, int end, InsiderFile file) {
        return PatternMatch.builder()
                .globalStartIndex(start)
                .globalEndIndex(end)
                .startIndex(file.getLineIndexFromFileIndex(start))
                .endIndex(file.getLineIndexFromFileIndex(end))
                .startLine(file.getLineNumberOfAbsoluteCharacterIndex(start))
                .endLine(file.getLineNumberOfAbsoluteCharacterIndex(end))
                .matchContent(file.getContent().substring(start, end))
                .build();
    }

    private void transformPatternIfWord() {
        if (pattern.contains("\\b")) {
            return;
        }
        pattern = String.format("\\b(%s)\\b", pattern);
    }

}
