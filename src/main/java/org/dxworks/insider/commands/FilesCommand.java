package org.dxworks.insider.commands;

import org.dxworks.insider.InsiderFile;

import java.util.List;

public interface FilesCommand extends InsiderCommand {

    default boolean acceptsFile(String path) {
        return true;
    }

    void init(List<String> commandArgs);

    //overriding execute to not need to implement it....this is not a good design!
    @Override
    default void execute(List<String> args) {}

    void analyse(InsiderFile file);

    void writeResults();
}
