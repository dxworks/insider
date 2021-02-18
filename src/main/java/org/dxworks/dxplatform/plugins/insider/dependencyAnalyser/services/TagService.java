package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services;

import lombok.Data;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Data
public class TagService {

    private Map<String, List<String>> tagsForFile;
    private Map<String, List<String>> tagsForCommentsInFile;
    private Map<String, List<String>> tagsWithoutCommentsInFile;

    public void getTagsForFile(List<Rule> rulesFromFiles, DependencyFile dependencyFile, List<MyFile> files) {
        List<MyPattern> patternsFromRule;
        tagsForFile = new HashMap<>();
        tagsForCommentsInFile = new HashMap<>();
        tagsWithoutCommentsInFile = new HashMap<>();

        List<Comment> comments = new CommentService().getComments();
        List<Language> languages = new LanguageService().getLanguages();

        for (MyFile file : files) {
            for(Rule rule : rulesFromFiles) {
                if(ruleForLanguage(rule, file.getExtension())) {
                    patternsFromRule = rule.getPatterns();

                    for (MyPattern myPattern : patternsFromRule) {
                        if (myPattern.getType().equals("regex-word")) {
                            transformPatternIfWord(myPattern);
                        }
                        if((myPattern.getScopes().contains("comment") && myPattern.getScopes().contains("code")) || myPattern.getScopes().contains("all")) {
                            if (dependencyFile.getMatchesInFile(Pattern.compile(myPattern.getPattern()), file.getFileContent()).size() != 0) {
                                putTagsForFile(file, rule, tagsForFile);
                            }
                        }
                        else if(myPattern.getScopes().contains("code")) {
                            try{if (dependencyFile.getMatchesInFileCodeOnly(Pattern.compile(myPattern.getPattern()), file, comments, languages).size() != 0) {
                                putTagsForFile(file, rule, tagsWithoutCommentsInFile);
                            }} catch(PatternSyntaxException e) {
                                System.out.println(file.getFileName() + "\n\n" + rule.getName());
                            }
                        }
                        else if(myPattern.getScopes().contains("comment")) {
                            if (dependencyFile.getMatchesInFileCommentsOnly(Pattern.compile(myPattern.getPattern()), file, comments, languages).size() != 0) {
                                putTagsForFile(file, rule, tagsForCommentsInFile);
                            }
                        }
                    }
                }
            }
        }
    }

    private void putTagsForFile(MyFile file, Rule rule, Map<String, List<String>> tagsForFile) {
        if (tagsForFile.containsKey(file.getFileName())) {
            List<String> newTags = new ArrayList<>(rule.getTags());
            newTags.addAll(tagsForFile.get(file.getFileName()));
            tagsForFile.put(file.getFileName(), newTags);
        } else {
            tagsForFile.put(file.getFileName(), rule.getTags());
        }
    }

    private static boolean ruleForLanguage(Rule rule, String language) {
        return rule.getApplies_to() == null || rule.getApplies_to().contains(language);
    }

    private static void transformPatternIfWord(MyPattern myPattern) {
        if(myPattern.getPattern().contains("\\b")){
            return;
        }

        String patternToModify = myPattern.getPattern();
        String modifiedPattern = "\\b(" + patternToModify + ")\\b";

        myPattern.setPattern(modifiedPattern);
    }
}
