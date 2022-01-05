package org.dxworks.insider.commands;

import lombok.extern.slf4j.Slf4j;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.configuration.InsiderConfiguration;

import java.util.List;

@Slf4j
public class VersionCommand implements NoFilesCommand {
    @Override
    public boolean parse(List<String> args) {
        if (args.size() != 1)
            return false;

        return VERSION.contains(args.get(0));
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, List<String> args) {

        System.out.println("Insider " + InsiderConfiguration.getInstance().getInsiderVersion());
    }

    @Override
    public String usage() {
        return "insider {-v | -version | --version | version}";
    }

    @Override
    public String getName() {
        return "version";
    }
}
