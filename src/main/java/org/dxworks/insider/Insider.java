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
import java.util.Map.Entry;
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
            helpCommand.execute(argsList);
            return;
        }

        if (versionCommand.parse(argsList)) {
            versionCommand.execute(argsList);
            return;
        }

        if (helpCommand.parse(argsList)) {
            helpCommand.execute(argsList);
            return;
        }

        List<List<String>> commands = extractCommands(args).stream()
                .filter(it -> !it.isEmpty())
                .collect(Collectors.toList());

        Map<List<String>, InsiderCommand> allCommandsConfig = commands.stream()
                .filter(it -> getInsiderCommand(it) != null)
                .collect(Collectors.toMap(Function.identity(), Insider::getInsiderCommand, (a, b) -> b, LinkedHashMap::new));

        readInsiderConfiguration();

        Map<List<String>, InsiderCommand> noFileCommands = allCommandsConfig.entrySet().stream()
                .filter(it -> it.getValue() instanceof NoFilesCommand)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        Map<List<String>, FilesCommand> fileCommands = allCommandsConfig.entrySet().stream()
                .filter(it -> it.getValue() instanceof FilesCommand)
                .collect(Collectors.toMap(Entry::getKey, t -> (FilesCommand) t.getValue()));

        executeNonFileCommands(noFileCommands);

        executeFileCommands(fileCommands);

        System.out.println("Insider " + InsiderConfiguration.getInstance().getInsiderVersion() + " finished analysis");
    }

    private static void executeNonFileCommands(Map<List<String>, InsiderCommand> noFileCommands) {
        noFileCommands.forEach((commandArgs, insiderCommand) -> {
            System.out.printf("*******************\nCommand %s\n*******************\n", insiderCommand.getName());

            boolean isValidInput = insiderCommand.parse(commandArgs);
            if (!isValidInput) {
                log.error("Input is not valid!");
                helpCommand.execute(commandArgs);
                return;
            }

            try {
                insiderCommand.execute(commandArgs);
            } catch (Exception e) {
                System.out.println("Command " + insiderCommand.getName() + "finished with errors");
                e.printStackTrace();
            }
        });
    }

    private static void executeFileCommands(Map<List<String>, FilesCommand> fileCommands) {
        List<FilesCommand> initialisedFileCommands = fileCommands.entrySet().stream()
                .filter(entry -> {
                    FilesCommand command = entry.getValue();
                    boolean isValid = command.parse(entry.getKey());
                    if (!isValid) {
                        System.out.printf("Incorrect configuration of command %s.\nCorrect usage: ", command.getName());
                        System.out.println(command.usage());
                    }
                    return isValid;
                })
                .peek(entry -> entry.getValue().init(entry.getKey()))
                .map(Entry::getValue).collect(Collectors.toList());

        readFilesAndExecute(initialisedFileCommands);
        initialisedFileCommands.forEach(FilesCommand::writeResults);
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

    private static void readFilesAndExecute(List<FilesCommand> commands) {
        Ignorer ignorer = new IgnorerBuilder(Paths.get(CONFIGURATION_FOLDER, ".ignore")).compile();

        readProjectFiles(InsiderConfiguration.getInstance().getRootFolder(), ignorer, commands);
    }

    private static void reportUnknownExtensions() {
        List<String> requiredLanguages = InsiderConfiguration.getInstance().getLanguages();
        requiredLanguages.stream()
                .filter(lang -> !LinguistService.getInstance().containsLanguage(lang))
                .forEach(lang -> System.out.println("Unknown language " + lang));
    }

    private static void readProjectFiles(String rootFolder, Ignorer ignorer, List<FilesCommand> commands) {
        try {
            List<Path> pathList = Files.walk(Paths.get(rootFolder))
                    .filter(Files::isRegularFile)
                    .filter(ignorer::accepts)
                    .collect(Collectors.toList());
            try (ProgressBar pb = new ProgressBar("Reading files", pathList.size(), ProgressBarStyle.ASCII)) {
                for (Path path : pathList) {
                    pb.step();
                    try {
                        InsiderFile insiderFile = InsiderFile.builder()
                                .path(path.toAbsolutePath().toString())
                                .name(path.getFileName().toString())
                                .extension(FilenameUtils.getExtension(path.getFileName().toString()))
                                .content(new String(Files.readAllBytes(path)))
                                .size(Files.size(path))
                                .build();
                        commands.stream().filter(c -> c.acceptsFile(insiderFile.getPath())).forEach(c -> c.analyse(insiderFile));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
