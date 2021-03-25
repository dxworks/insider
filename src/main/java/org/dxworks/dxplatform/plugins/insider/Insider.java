package org.dxworks.dxplatform.plugins.insider;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FilenameUtils;
import org.dxworks.dxplatform.plugins.insider.commands.*;
import org.dxworks.dxplatform.plugins.insider.configuration.InsiderConfiguration;
import org.dxworks.dxplatform.plugins.insider.technology.finder.LinguistService;
import org.dxworks.ignorerLibrary.Ignorer;
import org.dxworks.ignorerLibrary.IgnorerBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.dxworks.dxplatform.plugins.insider.commands.InsiderCommand.*;
import static org.dxworks.dxplatform.plugins.insider.constants.InsiderConstants.CONFIGURATION_FOLDER;
import static org.dxworks.dxplatform.plugins.insider.constants.InsiderConstants.RESULTS_FOLDER;

@Slf4j
public class Insider {

    private static HelpCommand helpCommand = new HelpCommand();
    private static VersionCommand versionCommand = new VersionCommand();

    public static void main(String[] args) {

        if (args == null) {
            System.err.println("Arguments cannot be null");
            return;
        }
        if (args.length == 0) {
            System.err.println("No command found");
            helpCommand.execute(null, args);
            return;
        }

        if (versionCommand.parse(args)) {
            versionCommand.execute(null, args);
            return;
        }

        if (helpCommand.parse(args)) {
            helpCommand.execute(null, args);
            return;
        }

        String command = args[0];

        InsiderCommand insiderCommand = getInsiderCommand(command);

        if (insiderCommand == null) {
            System.err.println("Invalid command!");
            helpCommand.execute(null, args);
            return;
        }

        boolean isValidInput = insiderCommand.parse(args);
        if (!isValidInput) {
            log.error("Input is not valid!");
            helpCommand.execute(null, args);
            return;
        }

        if (insiderCommand instanceof NoFilesCommand) {
            insiderCommand.execute(null, args);
        } else {
            List<InsiderFile> insiderFiles = readInsiderConfiguration(insiderCommand);
            insiderCommand.execute(insiderFiles, args);
        }

        System.out.println("Insider " + InsiderConfiguration.getInstance().getInsiderVersion() + " finished analysis");
    }

    private static InsiderCommand getInsiderCommand(String command) {
        switch (command) {
            case FIND:
                return new FindCommand();
            case DETECT:
                return new DetectCommand();
            case ADD:
                return new AddCommand();
            case DIAGNOSE:
                return new DiagnoseCommand();
            case CONVERT:
                return new ConvertCommand();
            case INSPECT:
                return new InspectCommand();
            default:
                return null;
        }
    }

    private static List<InsiderFile> readInsiderConfiguration(InsiderCommand insiderCommand) {
        File resultsFolder = new File(RESULTS_FOLDER);
        if (!resultsFolder.exists())
            resultsFolder.mkdirs();

        InsiderConfiguration.getInstance().load();

        String rootFolder = InsiderConfiguration.getInstance().getRootFolder();
        reportUnknownExtensions();

        Ignorer ignorer = new IgnorerBuilder(Paths.get(CONFIGURATION_FOLDER, ".ignore")).compile();
        return readProjectFiles(rootFolder, ignorer, insiderCommand);
    }

    private static void reportUnknownExtensions() {
        List<String> requiredLanguages = InsiderConfiguration.getInstance().getLanguages();
        requiredLanguages.stream()
                .filter(lang -> !LinguistService.getInstance().containsLanguage(lang))
                .forEach(lang -> System.out.println("Unknown language " + lang));
    }

    private static List<InsiderFile> readProjectFiles(String rootFolder, Ignorer ignorer, InsiderCommand insiderCommand) {
        List<InsiderFile> insiderFiles = new ArrayList<>();
        try {
            List<Path> pathList = Files.walk(Paths.get(rootFolder))
                    .filter(Files::isRegularFile)
                    .filter(ignorer::accepts)
                    .filter(path -> insiderCommand.acceptsFile(path.toString()))
                    .filter(Insider::hasAcceptedExtension)
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

    private static boolean hasAcceptedExtension(Path path) {
        LinguistService linguistService = LinguistService.getInstance();
        if (InsiderConfiguration.getInstance().getLanguages().isEmpty()) {
            return linguistService.hasAcceptedExtension(path.toString(), linguistService.getProgrammingLanguages());
        }

        return linguistService.hasAcceptedExtension(path.toString());
    }
}
