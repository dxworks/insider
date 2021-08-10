package org.dxworks.dxplatform.plugins.insider.commands;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.depext.DependencyExtractor;

import java.util.List;

public class ExtractCommand implements InsiderCommand {
    private final DependencyExtractor dependencyExtractor = new DependencyExtractor();

    @Override
    public boolean parse(String[] args) {
        return true;
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, String[] args) {
        dependencyExtractor.processAndWriteResults(insiderFiles);
    }

    @Override
    public String usage() {
        return null;
    }

    @Override
    public boolean acceptsFile(String path) {
        return dependencyExtractor.acceptFile(path);
    }
}
