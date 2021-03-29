package org.dxworks.dxplatform.plugins.insider.commands;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;
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
    List<String> VERSION = Arrays.asList("version", "-version", "--version", "-v");
    List<String> HELP = Arrays.asList("help", "-help", "--help", "-h");

    boolean parse(String[] args);

    default boolean fileExists(String filePath) {
        Path path = Paths.get(filePath);
        boolean result = Files.exists(path) && Files.isRegularFile(path);

        if (!result)
            log.error("Could not find file " + path.toAbsolutePath().toString());

        return result;
    }

    default boolean folderExists(String folderPath) {
        Path path = Paths.get(folderPath);
        boolean result = Files.exists(path) && Files.isDirectory(path);

        if (!result)
            log.error("Could not find folder " + path.toAbsolutePath().toString());

        return result;
    }

    default boolean acceptsFile(String path) {
        return true;
    }

    void execute(List<InsiderFile> insiderFiles, String[] args);

    String usage();
}
