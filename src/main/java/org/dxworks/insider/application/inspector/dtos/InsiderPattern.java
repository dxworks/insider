package org.dxworks.insider.application.inspector.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.IntRange;
import org.dxworks.insider.InsiderFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import static java.util.regex.Pattern.*;

@Slf4j
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
    private boolean hasErrors = false;
    @JsonIgnore
    private int regexFlags = -1;

    public List<PatternMatch> getMatches(InsiderFile file, List<IntRange> commentRanges) {
        if (hasErrors)
            return Collections.emptyList();

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
        List<PatternMatch> matches = new ArrayList<>();
        try {
            Matcher matcher = Pattern.compile(pattern, createModifier()).matcher(file.getContent());
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                PatternMatch patternMatch = createPatternMatch(start, end, file);
                matches.add(patternMatch);
            }
        } catch (PatternSyntaxException e) {
            hasErrors = true;
            System.out.println();
            log.error("Could not compile pattern '{}' due to: {}", pattern, e.getMessage());
        }
        return matches;
    }

    private int createModifier() {
        if(regexFlags != -1)
            return regexFlags;

        regexFlags = 0 ;

        if(modifiers == null)
            modifiers = new ArrayList<>();

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
