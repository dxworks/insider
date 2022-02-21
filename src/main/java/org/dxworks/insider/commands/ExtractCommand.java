package org.dxworks.insider.commands;

import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.depext.DependencyExtractor;

import java.util.List;

public class ExtractCommand implements FilesCommand {
    private final DependencyExtractor dependencyExtractor = new DependencyExtractor();

    @Override
    public boolean parse(List<String> args) {
        return true;
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

    @Override
    public void init(List<String> commandArgs) {
    }

    @Override
    public void analyse(InsiderFile file) {
        dependencyExtractor.execute(file);
    }

    @Override
    public void writeResults() {
        dependencyExtractor.generateResults();
    }
}
