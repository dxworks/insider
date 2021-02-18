package org.dxworks.dxplatform.plugins.insider.commands;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.MyFile;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.Rule;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services.DependencyFile;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services.FileService;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services.RuleService;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services.TagService;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InspectCommand implements NoFilesCommand {
    protected String projectPath;

    private boolean pathExists(String path) {
        return Paths.get(path).toFile().isDirectory();
    }

    @Override
    public boolean parse(String[] args) {
        if (args.length != 2)
            return false;

        projectPath = args[1];

        return pathExists(projectPath);
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, String[] args) {
        RuleService ruleService = new RuleService();
        FileService fileService = new FileService();
        TagService tagService = new TagService();
        DependencyFile dependencyFile = new DependencyFile();

        List<Rule> rulesFromFiles = ruleService.getRulesFromFile(getClass().getResource("/rules").getPath().substring(1));
        List<MyFile> files = fileService.getFilesFromFolder(projectPath);

        tagService.getTagsForFile(rulesFromFiles, dependencyFile, files);

        Map<String, List<String>> tagsForFile = tagService.getTagsForFile();
        Map<String, List<String>> tagsForCommentsInFile = tagService.getTagsForCommentsInFile();
        Map<String, List<String>> tagsWithoutCommentsInFile = tagService.getTagsWithoutCommentsInFile();

        List<String> allTags = new ArrayList<>();

        System.out.println("Tags in entire file:");
        for (Map.Entry<String, List<String>> entry : tagsForFile.entrySet()) {
            allTags.addAll(entry.getValue());
            System.out.println("File: " + entry.getKey() + " has following tags:\n\t" + entry.getValue());
        }
        System.out.println("--------------------------------------------------\n");

        System.out.println("Tags in comments from file:");
        for (Map.Entry<String, List<String>> entry : tagsForCommentsInFile.entrySet()) {
            allTags.addAll(entry.getValue());
            System.out.println("File: " + entry.getKey() + " has following tags:\n\t" + entry.getValue());
        }
        System.out.println("--------------------------------------------------\n");

        System.out.println("Tags in code from file:");
        for (Map.Entry<String, List<String>> entry : tagsWithoutCommentsInFile.entrySet()) {
            allTags.addAll(entry.getValue());
            System.out.println("File: " + entry.getKey() + " has following tags:\n\t" + entry.getValue());
        }
        System.out.println("--------------------------------------------------\n");

        Map<String, Long> counts =
                allTags.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            System.out.println("Tag: " + entry.getKey() + " appears: " + entry.getValue() + " times");
        }
    }

    @Override
    public String usage() {
        return "insider analyse <path_to_project>";
    }
}
