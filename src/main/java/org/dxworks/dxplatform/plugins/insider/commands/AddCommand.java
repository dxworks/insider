package org.dxworks.dxplatform.plugins.insider.commands;

import lombok.extern.slf4j.Slf4j;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.utils.transformers.JsonToCsvTransformer;

import java.io.IOException;
import java.util.List;

@Slf4j
public class AddCommand extends ConvertCommand {

    @Override
    public boolean parse(String[] args) {
        return super.parse(args) && sourceFile.endsWith(".csv") && targetFile.endsWith(".json");
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, String[] args) {
        JsonToCsvTransformer jsonToCsvTransformer = new JsonToCsvTransformer();

        try {
            jsonToCsvTransformer.addCsvFileToJson(sourceFile, targetFile);
        } catch (IOException e) {
            log.error("Could not transform file!", e);
        }
    }

    @Override
    public String usage() {
        return "insider add <path_to_csv> <path_to_json>";
    }
}
