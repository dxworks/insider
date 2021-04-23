package org.dxworks.dxplatform.plugins.insider.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.lang.math.IntRange;
import org.dxworks.dxplatform.plugins.insider.InspectResult;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.InsiderResult;
import org.dxworks.dxplatform.plugins.insider.configuration.InsiderConfiguration;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.Rule;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services.CommentService;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services.RuleService;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dxworks.dxplatform.plugins.insider.constants.InsiderConstants.PROJECT_ID;
import static org.dxworks.dxplatform.plugins.insider.constants.InsiderConstants.RESULTS_FOLDER;

@Slf4j
public class InspectCommand implements InsiderCommand {

    private List<String> ruleFiles;

    @Override
    public boolean parse(String[] args) {
        if (args.length == 1)
            return false;

        String[] files = Arrays.copyOfRange(args, 1, args.length);
        ruleFiles = Arrays.stream(files).filter(filePath -> folderExists(filePath) || fileExists(filePath)).collect(Collectors.toList());

        return !ruleFiles.isEmpty() && files.length == ruleFiles.size();
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, String[] args) {
        RuleService ruleService = new RuleService();
        List<Rule> rules = ruleService.getRuleFromFiles(ruleFiles);
        rules.forEach(Rule::transformPatterns);

        List<InsiderResult> insiderResults;

        try (ProgressBar pb = new ProgressBarBuilder()
                .setInitialMax(insiderFiles.size())
                .setUnit(" Files", 1)
                .setTaskName("Inspecting...")
                .setStyle(ProgressBarStyle.ASCII)
                .setUpdateIntervalMillis(100)
                .setPrintStream(System.err)
                .build()) {
            insiderResults = insiderFiles.parallelStream()
                    .flatMap(insiderFile -> {
                        List<IntRange> commentRanges = getCommentRanges(insiderFile);

                        Stream<InsiderResult> insiderResultStream = rules.parallelStream()
                                .flatMap(rule -> rule.analyze(insiderFile, commentRanges).stream());
                        pb.step();
                        return insiderResultStream;
                    })
                    .collect(Collectors.toList());

            List<InspectResult> result = insiderResults.stream().map(insiderResult -> new InspectResult(insiderResult.getFile(), insiderResult.getName(), insiderResult.getValue())).collect(Collectors.toList());

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(RESULTS_FOLDER, InsiderConfiguration.getInstance().getProperty(PROJECT_ID) + "-tags.json").toFile(), insiderResults);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(RESULTS_FOLDER, InsiderConfiguration.getInstance().getProperty(PROJECT_ID) + "-inspect-tags.json").toFile(), result);
        } catch (IOException e) {
            log.error("Inspect command finished unsuccessfully!", e);
        }
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
}
