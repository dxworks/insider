package org.dxworks.insider.commands;

import org.dxworks.insider.InsiderFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public interface InsiderCommand {

    Logger log = LoggerFactory.getLogger(InsiderCommand.class);

    String DETECT = "detect";
    String FIND = "find";
    String ADD = "add";
    String DIAGNOSE = "diagnose";
    String CONVERT = "convert";
    String INSPECT = "inspect";
    String EXTRACT = "extract";
    String INDENT = "indent";
    String MEASURE = "measure";
    List<String> VERSION = Arrays.asList("version", "-version", "--version", "-v");
    List<String> HELP = Arrays.asList("help", "-help", "--help", "-h");

    boolean parse(List<String> args);

    default boolean fileExists(String filePath) {
        Path path = Path.of(filePath).toAbsolutePath();

        if (!Files.exists(path)) {
            log.error("File " + path.toAbsolutePath() + " does not exist");
            return false;
        }

        if (!Files.isRegularFile(path)) {
            log.error("File " + path.toAbsolutePath() + " is not a file!");
            return false;
        }

        return true;
    }

    default boolean folderExists(String folderPath) {
        Path path = Paths.get(folderPath).toAbsolutePath();
        if (!Files.exists(path)) {
            log.error("Folder " + path.toAbsolutePath() + " does not exist");
            return false;
        }

        if (!Files.isDirectory(path)) {
            log.error("Folder " + path.toAbsolutePath() + " is not a firectory!");
            return false;
        }

        return true;
    }

    void execute(List<String> args);

    String usage();

    String getName();
}
