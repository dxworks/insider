package org.dxworks.insider.commands;

import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.depext.DependencyExtractor;

import java.util.List;

public class ExtractCommand implements InsiderCommand {
    private final DependencyExtractor dependencyExtractor = new DependencyExtractor();

    @Override
    public boolean parse(List<String> args) {
        return true;
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, List<String> args) {
        dependencyExtractor.processAndWriteResults(insiderFiles);
    }

    @Override
    public String usage() {
        return "insider extract";
    }

    @Override
    public String getName() {
        return EXTRACT;
    }

    @Override
    public boolean acceptsFile(String path) {
        return dependencyExtractor.acceptFile(path);
    }
}
