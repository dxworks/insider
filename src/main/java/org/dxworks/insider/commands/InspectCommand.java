package org.dxworks.insider.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.IntRange;
import org.dxworks.insider.ChronosTag;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.InsiderResult;
import org.dxworks.insider.application.inspector.dtos.Rule;
import org.dxworks.insider.application.inspector.services.CommentService;
import org.dxworks.insider.application.inspector.services.RuleService;
import org.dxworks.insider.configuration.InsiderConfiguration;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dxworks.insider.constants.InsiderConstants.RESULTS_FOLDER;

@Slf4j
public class InspectCommand implements FilesCommand {

    private List<String> ruleFiles;

    private List<Rule> rules;

    private List<InsiderResult> insiderResults = new ArrayList<>();

    @Override
    public boolean parse(List<String> args) {
        if (args.size() == 1)
            return false;

        List<String> files = args.subList(1, args.size());
        ruleFiles = files.stream().filter(filePath -> folderExists(filePath) || fileExists(filePath)).collect(Collectors.toList());

        return !ruleFiles.isEmpty() && files.size() == ruleFiles.size();
    }

    private List<IntRange> getCommentRanges(InsiderFile insiderFile) {
        List<IntRange> commentRanges = new ArrayList<>();
        CommentService commentService = CommentService.getInstance();
        commentRanges.addAll(commentService.extractInlineCommentLines(insiderFile));
        commentRanges.addAll(commentService.extractMultilineCommentLines(insiderFile));
        return commentRanges;
    }

    @Override
    public String usage() {
        return "insider inspect <paths_to_rule>...";
    }

    @Override
    public String getName() {
        return INSPECT;
    }

    @Override
    public void init(List<String> commandArgs) {
        RuleService ruleService = new RuleService();
        rules = ruleService.getRuleFromFiles(ruleFiles);
        rules.forEach(Rule::transformPatterns);
    }

    @Override
    public void analyse(InsiderFile file) {
        List<IntRange> commentRanges = getCommentRanges(file);

        insiderResults.addAll(
                rules.parallelStream()
                .flatMap(rule -> rule.analyze(file, commentRanges).stream())
                .collect(Collectors.toList()));
    }

    @Override
    public void writeResults() {
        try {
            List<ChronosTag> chronosTags = insiderResults.stream().map(insiderResult -> new ChronosTag(insiderResult.getFile(), "appinspector." + insiderResult.getName(), insiderResult.getValue())).collect(Collectors.toList());
            Map<String, Map<String, List<ChronosTag>>> chronosResult = Map.of("file", Map.of("concerns", chronosTags));

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(RESULTS_FOLDER, InsiderConfiguration.getInstance().getProjectID() + "-tags.json").toFile(), insiderResults);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(RESULTS_FOLDER, InsiderConfiguration.getInstance().getProjectID() + "-chronos-tags.json").toFile(), chronosResult);
        } catch (IOException e) {
            log.error("Inspect command finished unsuccessfully!", e);
        }
    }
}
