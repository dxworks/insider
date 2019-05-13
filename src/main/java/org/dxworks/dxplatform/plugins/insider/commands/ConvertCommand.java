package org.dxworks.dxplatform.plugins.insider.commands;

import lombok.extern.slf4j.Slf4j;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.utils.transformers.JsonToCsvTransformer;

import java.io.IOException;
import java.util.List;

@Slf4j
public class ConvertCommand implements NoFilesCommand {
    protected String sourceFile;
    protected String targetFile;

    @Override
    public boolean parse(String[] args) {
        if (args.length != 3)
            return false;

        sourceFile = args[1];
        targetFile = args[2];

        return fileExists(sourceFile) && fileExists(targetFile);
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, String[] args) {
        JsonToCsvTransformer jsonToCsvTransformer = new JsonToCsvTransformer();

        try {
            jsonToCsvTransformer.transformJsonFileToCsv(sourceFile, targetFile);
        } catch (IOException e) {
            log.error("Could not transform file!", e);
        }
    }

    @Override
    public String usage() {
        return "insider convert <path_to_json> <path_to_csv>";
    }
}
