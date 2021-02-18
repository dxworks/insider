package org.dxworks.dxplatform.plugins.insider.commands;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public interface InsiderCommand {
    String DETECT = "detect";
    String FIND = "find";
    String ADD = "add";
    String DIAGNOSE = "diagnose";
    String CONVERT = "convert";
    String ANALYSE = "analyse";
    List<String> VERSION = Arrays.asList("version", "-version", "--version", "-v");
    List<String> HELP = Arrays.asList("help", "-help", "--help", "-h");

    boolean parse(String[] args);

    default boolean fileExists(String filePath) {
        return Paths.get(filePath).toFile().exists();
    }

    void execute(List<InsiderFile> insiderFiles, String[] args);

    String usage();
}
