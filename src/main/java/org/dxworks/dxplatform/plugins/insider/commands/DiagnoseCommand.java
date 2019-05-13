package org.dxworks.dxplatform.plugins.insider.commands;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.utils.transformers.JsonToCsvTransformer;

import java.util.List;

public class DiagnoseCommand implements NoFilesCommand {

    private String file;

    @Override
    public boolean parse(String[] args) {
        if (args.length != 2)
            return false;

        file = args[1];

        return fileExists(file);
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, String[] args) {
        JsonToCsvTransformer jsonToCsvTransformer = new JsonToCsvTransformer();

        jsonToCsvTransformer.diagnoseJsonFile(file);
    }

    @Override
    public String usage() {
        return "insider diagnose <path_to_json>";
    }
}
