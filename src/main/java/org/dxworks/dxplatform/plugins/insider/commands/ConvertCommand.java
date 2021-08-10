package org.dxworks.dxplatform.plugins.insider.commands;

import lombok.extern.slf4j.Slf4j;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.technology.finder.model.Technology;
import org.dxworks.dxplatform.plugins.insider.technology.finder.parsers.JavaLibrariesCsvParser;
import org.dxworks.dxplatform.plugins.insider.technology.finder.parsers.JsonFingerprintParser;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class ConvertCommand implements NoFilesCommand {
    protected String sourceFile;
    protected String targetFile;

    @Override
    public boolean parse(List<String> args) {
        if (args.size() != 3)
            return false;

        sourceFile = args.get(1);
        targetFile = args.get(2);

        return fileExists(sourceFile);
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, List<String> args) {
        try {
            transformJsonFileToCsv(sourceFile, targetFile);
        } catch (IOException e) {
            log.error("Could not transform file!", e);
        }
    }

    private void transformJsonFileToCsv(String jsonFile, String csvFile) throws IOException {
        JsonFingerprintParser jsonFingerprintParser = new JsonFingerprintParser();
        List<Technology> technologies = jsonFingerprintParser.parseTechnologiesFile(jsonFile);

        JavaLibrariesCsvParser javaLibrariesCsvParser = new JavaLibrariesCsvParser();
        javaLibrariesCsvParser.writeTechnologiesToFile(technologies, Paths.get(csvFile));
    }

    @Override
    public String usage() {
        return "insider convert <path_to_json> <path_to_csv>";
    }

    @Override
    public String getName() {
        return CONVERT;
    }
}
