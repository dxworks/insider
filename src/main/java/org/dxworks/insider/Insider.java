package org.dxworks.insider;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.dxworks.ignorerLibrary.Ignorer;
import org.dxworks.ignorerLibrary.IgnorerBuilder;
import org.dxworks.insider.commands.*;
import org.dxworks.insider.configuration.InsiderConfiguration;
import org.dxworks.insider.technology.finder.LinguistService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.dxworks.insider.constants.InsiderConstants.CONFIGURATION_FOLDER;
import static org.dxworks.insider.constants.InsiderConstants.RESULTS_FOLDER;

@Slf4j
public class Insider {

    private static HelpCommand helpCommand = new HelpCommand();
    private static VersionCommand versionCommand = new VersionCommand();

    public static void main(String[] args) {

        if (args == null) {
            System.err.println("Arguments cannot be null");
            return;
        }

        List<String> argsList = List.of(args);
        InsiderConfiguration.getInstance().readInsiderVersion();

        if (args.length == 0) {
            System.err.println("No command found");
            helpCommand.execute(null, argsList);
            return;
        }

        if (versionCommand.parse(argsList)) {
            versionCommand.execute(null, argsList);
            return;
        }

        if (helpCommand.parse(argsList)) {
            helpCommand.execute(null, argsList);
            return;
        }

        List<List<String>> commands = extractCommands(args).stream()
                .filter(it -> !it.isEmpty())
                .collect(Collectors.toList());

        Map<List<String>, InsiderCommand> allCommandsConfig = commands.stream()
                .filter(it -> getInsiderCommand(it) != null)
                .collect(Collectors.toMap(Function.identity(), Insider::getInsiderCommand, (a, b) -> b, LinkedHashMap::new));

        readInsiderConfiguration();
        List<InsiderFile> insiderFiles = allCommandsConfig.values()
                .stream()
                .allMatch(it -> it instanceof NoFilesCommand)
                ? new ArrayList<>()
                : getInsiderFiles(allCommandsConfig);

        AtomicInteger index = new AtomicInteger(1);
        int commandsSize = allCommandsConfig.size();
        allCommandsConfig.forEach((commandArgs, insiderCommand) -> {
            System.out.printf("*******************\nCommand %s (%d/%d) \n*******************\n", insiderCommand.getName(), index.getAndIncrement(), commandsSize);

            boolean isValidInput = insiderCommand.parse(commandArgs);
            if (!isValidInput) {
                log.error("Input is not valid!");
                helpCommand.execute(null, commandArgs);
                return;
            }

            List<InsiderFile> acceptedFiles =
                    insiderFiles.stream().filter(it -> hasAcceptedExtension(it.getPath())).collect(Collectors.toList());

            try {
                if (insiderCommand instanceof NoFilesCommand) {
                    insiderCommand.execute(null, commandArgs);
                } else if (insiderCommand instanceof AllFilesCommand) {
                    insiderCommand.execute(insiderFiles, commandArgs);
                } else {
                    insiderCommand.execute(acceptedFiles, commandArgs);
                }
            } catch (Exception e) {
                System.out.println("Command " + insiderCommand.getName() + "finished with errors");
                e.printStackTrace();
            }
        });

        System.out.println("Insider " + InsiderConfiguration.getInstance().getInsiderVersion() + " finished analysis");
    }

    private static List<List<String>> extractCommands(String[] args) {
        List<List<String>> commands = new ArrayList<>();
        List<String> currentCommand = new ArrayList<>();
        for (String arg : args) {
            if ("AND".equals(arg)) {
                commands.add(currentCommand);
                currentCommand = new ArrayList<>();
                continue;
            }
            currentCommand.add(arg);
        }
        commands.add(currentCommand);

        return commands;
    }

    private static InsiderCommand getInsiderCommand(List<String> commandArgs) {
        if (CollectionUtils.isEmpty(commandArgs))
            return null;
        switch (commandArgs.get(0)) {
            case InsiderCommand.FIND:
                return new FindCommand();
            case InsiderCommand.DETECT:
                return new DetectCommand();
            case InsiderCommand.ADD:
                return new AddCommand();
            case InsiderCommand.DIAGNOSE:
                return new DiagnoseCommand();
            case InsiderCommand.CONVERT:
                return new ConvertCommand();
            case InsiderCommand.INSPECT:
                return new InspectCommand();
            case InsiderCommand.EXTRACT:
                return new ExtractCommand();
            case InsiderCommand.INDENT:
                return new IndentationCount();
            case InsiderCommand.MEASURE:
                return new MeasureCommand();
            case InsiderCommand.COUNT:
                return new ClocCommand();
            default:
                return null;
        }
    }

    private static void readInsiderConfiguration() {
        File resultsFolder = new File(RESULTS_FOLDER);
        if (!resultsFolder.exists())
            resultsFolder.mkdirs();

        InsiderConfiguration.getInstance().load();
        reportUnknownExtensions();
    }

    private static List<InsiderFile> getInsiderFiles(Map<List<String>, InsiderCommand> insiderCommand) {
        Ignorer ignorer = new IgnorerBuilder(Paths.get(CONFIGURATION_FOLDER, ".ignore")).compile();

        if (!insiderCommand.values().stream().allMatch(it -> it instanceof NoFilesCommand))
            return readProjectFiles(InsiderConfiguration.getInstance().getRootFolder(), ignorer);
        else return new ArrayList<>();
    }

    private static void reportUnknownExtensions() {
        List<String> requiredLanguages = InsiderConfiguration.getInstance().getLanguages();
        requiredLanguages.stream()
                .filter(lang -> !LinguistService.getInstance().containsLanguage(lang))
                .forEach(lang -> System.out.println("Unknown language " + lang));
    }

    private static List<InsiderFile> readProjectFiles(String rootFolder, Ignorer ignorer) {
        List<InsiderFile> insiderFiles = new ArrayList<>();
        try {
            List<Path> pathList = Files.walk(Paths.get(rootFolder))
                    .filter(Files::isRegularFile)
                    .filter(ignorer::accepts)
                    .collect(Collectors.toList());
            try (ProgressBar pb = new ProgressBar("Reading files", pathList.size(), ProgressBarStyle.ASCII)) {
                for (Path path : pathList) {
                    pb.step();
                    try {
                        insiderFiles.add(InsiderFile.builder()
                                .path(path.toAbsolutePath().toString())
                                .name(path.getFileName().toString())
                                .extension(FilenameUtils.getExtension(path.getFileName().toString()))
                                .content(new String(Files.readAllBytes(path)))
                                .size(Files.size(path))
                                .build());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return insiderFiles;
    }

    private static boolean hasAcceptedExtension(String filePath) {
        LinguistService linguistService = LinguistService.getInstance();
        if (InsiderConfiguration.getInstance().getLanguages().isEmpty()) {
            return linguistService.hasAcceptedExtension(filePath, linguistService.getProgrammingLanguages());
        }

        return linguistService.hasAcceptedExtension(filePath);
    }
}
