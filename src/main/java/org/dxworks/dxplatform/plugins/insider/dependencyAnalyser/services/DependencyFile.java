package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.Comment;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.Language;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.MyFile;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.PatternMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyFile {

    private List<Integer> lineBreaks;
    private List<Integer> inlineComments;
    private List<ImmutablePair<Integer, Integer>> multilineComments;

    public List<PatternMatch> getMatchesInFile(Pattern pattern, String content) {
        extractLineBreaks(content);

        Matcher matcher = pattern.matcher(content);

        List<PatternMatch> matches = new ArrayList<>();

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            PatternMatch patternMatch = getPatternMatch(start, end, content);

            matches.add(patternMatch);
        }
        return matches;
    }

    private int getLineNumberOfAbsoluteCharacterIndex(int index) {
        for (int i = 0; i < lineBreaks.size(); i++) {
            if (index <= lineBreaks.get(i))
                return i;
        }
        return lineBreaks.size();
    }

    private void extractLineBreaks(String content) {
        lineBreaks = new ArrayList<>();
        lineBreaks.add(-1);
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lineBreaks.add(i);
            }
        }
    }

    private void extractInlineComments(String content, String language, List<Comment> comments) {
        inlineComments = new ArrayList<>();
        Comment inlineComment = null;

        for (Comment comment : comments) {
            if (comment.getLanguage().contains(language)) {
                inlineComment = comment;
            }
        }

        if (inlineComment != null) {
            if (inlineComment.getInline().equals("//")) {
                Matcher matcher = Pattern.compile("//").matcher(content);
                while (matcher.find()) {
                    int start = matcher.start();

                    Integer startLine = getLineNumberOfAbsoluteCharacterIndex(start);
                    inlineComments.add(startLine);
                }
            }
        }
    }

    private void extractMultilineComments(String content, String language, List<Comment> comments) {
        multilineComments = new ArrayList<>();
        Comment multilineComment = null;
        List<Integer> preffixes = new ArrayList<>();
        List<Integer> suffixes = new ArrayList<>();

        for (Comment comment : comments) {
            if (comment.getLanguage().contains(language)) {
                multilineComment = comment;
            }
        }

        List<ImmutablePair<Integer, Integer>> commentIndexes = new ArrayList<>();
        Matcher commentMatcher = Pattern.compile("\"").matcher(content);
        List<Integer> preffixesToRemove = new ArrayList<>();

        while (commentMatcher.find()) {
            int commentStartIndex = commentMatcher.start();
            Integer commentStartLine = getLineNumberOfAbsoluteCharacterIndex(commentStartIndex);
            commentIndexes.add(new ImmutablePair<>(commentStartLine, commentStartIndex));
        }

        if (multilineComment != null) {
            if (multilineComment.getPreffix().equals("/*")) {
                Matcher matcher = Pattern.compile("/\\*").matcher(content);
                while (matcher.find()) {
                    int start = matcher.start();

                    Integer startLine = getLineNumberOfAbsoluteCharacterIndex(start);
                    preffixes.add(startLine);

                    for (int i = 0; i < commentIndexes.size() - 1; i++) {
                        if (commentIndexes.get(i).getLeft().equals(startLine) && commentIndexes.get(i + 1).getLeft().equals(startLine)) {
                            if (commentIndexes.get(i).getRight() < start && commentIndexes.get(i + 1).getRight() > start) {
                                preffixesToRemove.add(startLine);
                            }
                        }
                    }
                }
            }
            if (multilineComment.getSuffix().equals("*/")) {
                Matcher matcher = Pattern.compile("\\*/").matcher(content);
                while (matcher.find()) {
                    int start = matcher.start();

                    Integer startLine = getLineNumberOfAbsoluteCharacterIndex(start);
                    suffixes.add(startLine);
                }
            }
        }

        for (Integer i : preffixesToRemove) {
            preffixes.remove(i);
        }

        for (int i = 0; i < preffixes.size(); i++) {
            multilineComments.add(new ImmutablePair<>(preffixes.get(i), suffixes.get(i)));
        }
    }

    public List<PatternMatch> getMatchesInFileCodeOnly(Pattern pattern, MyFile file, List<Comment> comments, List<Language> languages) {
        extractLineBreaks(file.getFileContent());

        String fileExtension = "." + file.getExtension();

        extractCommentLines(file, comments, languages, fileExtension);

        Matcher matcher = pattern.matcher(file.getFileContent());

        List<PatternMatch> matches = new ArrayList<>();
        boolean isInMultilineComment = false;
        boolean isInInlineComment = false;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            PatternMatch patternMatch = getPatternMatch(start, end, file.getFileContent());

            for (ImmutablePair<Integer, Integer> pair : multilineComments) {
                if (patternMatch.getStartLine() >= pair.getKey() && patternMatch.getEndLine() <= pair.getValue()) {
                    isInMultilineComment = true;
                    break;
                }
            }

            if (inlineComments.contains(patternMatch.getStartLine())) {
                isInInlineComment = true;
            }

            if (!isInInlineComment && !isInMultilineComment) {
                matches.add(patternMatch);
            }
        }
        return matches;
    }

    private PatternMatch getPatternMatch(int start, int end, String fileContent) {
        return PatternMatch.builder()
                .globalStartIndex(start)
                .globalEndIndex(end)
                .startIndex(start - lineBreaks.get(getLineNumberOfAbsoluteCharacterIndex(start) - 1))
                .endIndex(end - lineBreaks.get(getLineNumberOfAbsoluteCharacterIndex(end) - 1))
                .startLine(getLineNumberOfAbsoluteCharacterIndex(start))
                .endLine(getLineNumberOfAbsoluteCharacterIndex(end))
                .matchContent(fileContent.substring(start, end))
                .build();
    }

    public List<PatternMatch> getMatchesInFileCommentsOnly(Pattern pattern, MyFile file, List<Comment> comments, List<Language> languages) {
        extractLineBreaks(file.getFileContent());

        String fileExtension = "." + file.getExtension();

        extractCommentLines(file, comments, languages, fileExtension);

        Matcher matcher = pattern.matcher(file.getFileContent());

        List<PatternMatch> matches = new ArrayList<>();

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            PatternMatch patternMatch = getPatternMatch(start, end, file.getFileContent());

            for (ImmutablePair<Integer, Integer> pair : multilineComments) {
                if (patternMatch.getStartLine() >= pair.getKey() && patternMatch.getEndLine() <= pair.getValue()) {
                    matches.add(patternMatch);
                }
            }

            if (inlineComments.contains(patternMatch.getStartLine())) {
                matches.add(patternMatch);
            }
        }
        return matches;
    }

    private void extractCommentLines(MyFile file, List<Comment> comments, List<Language> languages, String fileExtension) {
        for (Language language : languages) {
            if (language.getExtensions().contains(fileExtension)) {
                String languageName = language.getName();
                extractInlineComments(file.getFileContent(), languageName, comments);
                extractMultilineComments(file.getFileContent(), languageName, comments);
            }
            if (inlineComments == null && multilineComments == null) {
                inlineComments = Collections.emptyList();
                multilineComments = Collections.emptyList();
            }
        }
    }
}
