package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.IntRange;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.Comment;
import org.dxworks.dxplatform.plugins.insider.exceptions.InsiderException;
import org.dxworks.dxplatform.plugins.insider.technology.finder.LinguistService;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class CommentService {

    private static final String COMMENTS_FILE_NAME = "comments.json";
    private static CommentService _instance = null;

    private List<Comment> comments;

    private CommentService() {
        loadComments();
    }

    public static CommentService getInstance() {
        if (_instance == null)
            _instance = new CommentService();
        return _instance;
    }

    public void loadComments() {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            comments = objectMapper.readValue(Paths.get("config", COMMENTS_FILE_NAME).toFile(), new TypeReference<List<Comment>>() {
            });
        } catch (IOException e) {
            throw new InsiderException(
                    "Could not load comment configuration from file. Please make sure a file called "
                            + COMMENTS_FILE_NAME + " exists in the Insider config directory.", e);
        }
    }

    public List<IntRange> extractInlineCommentLines(InsiderFile file) {
        List<IntRange> inlineComments = new ArrayList<>();

        Comment commentStyle = getCommentForFile(file);
        if (commentStyle != null) {
            String inlineCommentPrefix = commentStyle.getInline();
            if (inlineCommentPrefix != null) {
                Matcher matcher = Pattern.compile(inlineCommentPrefix).matcher(file.getContent());
                while (matcher.find()) {
                    int start = matcher.start();
                    inlineComments.add(new IntRange(start, file.getLineBreaks().get(file.getLineNumberOfAbsoluteCharacterIndex(start)).intValue()));
                }
            }
        }

        return inlineComments;
    }

    private Comment getCommentForFile(InsiderFile file) {
        List<String> languages = LinguistService.getInstance().getLanguagesForFile(file);

        return comments.stream().filter(comment -> comment.getLanguages().stream().anyMatch(languages::contains)).findFirst().orElse(null);
    }

    public List<IntRange> extractMultilineCommentLines(InsiderFile file) {
        List<IntRange> multilineCommentLinePairs = new ArrayList<>();

        Comment commentStyle = getCommentForFile(file);
        if (commentStyle != null) {
            String multilineCommentPrefix = commentStyle.getPreffix();
            String multilineCommentSuffix = commentStyle.getSuffix();
            if (multilineCommentPrefix != null && multilineCommentSuffix != null) {
                List<IntRange> enclosingQuotes = new ArrayList<>();
                String content = file.getContent();

                List<Integer> quotesIndex = getIndexesForSubstring(file.getContent(), "\"");
                if (quotesIndex.size() % 2 == 0) {
                    for (int i = 0; i < quotesIndex.size(); i += 2) {
                        enclosingQuotes.add(new IntRange(quotesIndex.get(i), quotesIndex.get(i + 1)));
                    }
                } else {
                    log.warn("Could not get Quotes because we found odd number of quotes in file: " + file.getPath());
                }

                List<Integer> prefixCommentIndexes = getIndexesForSubstring(content, "/*").stream()
                        .filter(index -> isNotInQuotes(index, enclosingQuotes))
                        .collect(Collectors.toList());
                List<Integer> suffixCommentIndexes = getIndexesForSubstring(content, "*/").stream()
                        .filter(index -> isNotInQuotes(index, enclosingQuotes))
                        .collect(Collectors.toList());

                if (prefixCommentIndexes.size() != suffixCommentIndexes.size()) {
                    log.warn("Comments not well matched for file: " + file.getPath() + "!\nPrefix indexes: " + prefixCommentIndexes + "\nSuffix indexes: " + suffixCommentIndexes);
                }
                for (int i = 0; i < Math.min(prefixCommentIndexes.size(), suffixCommentIndexes.size()); i++) {
                    multilineCommentLinePairs.add(new IntRange(prefixCommentIndexes.get(i), suffixCommentIndexes.get(i)));
                }
            }
        }
        return multilineCommentLinePairs;
    }

    private boolean isNotInQuotes(Integer index, List<IntRange> enclosingQuotes) {
        return enclosingQuotes.stream().anyMatch(interval -> interval.containsInteger(index));
    }

    private List<Integer> getIndexesForSubstring(String content, String substring) {
        List<Integer> globalIndexes = new ArrayList<>();
        int index = 0;
        while (index != -1) {
            index = content.indexOf(substring, index);
            if (index != -1) {
                globalIndexes.add(index);
                index++;
            }
        }
        return globalIndexes;
    }

    private int getBackslashesBeforeQuote(String content, int quoteIndex) {
        String substringToQuote = content.substring(0, quoteIndex);
        int numberOfBackslashes = 0;
        for (int i = substringToQuote.length() - 1; i >= 0; i--) {
            if (substringToQuote.charAt(i) == '\\')
                numberOfBackslashes++;
            else break;
        }
        return numberOfBackslashes;
    }
}
