package org.dxworks.dxplatform.plugins.insider.commands;

import lombok.extern.slf4j.Slf4j;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.configuration.InsiderConfiguration;

import java.io.IOException;
import java.util.List;

@Slf4j
public class VersionCommand implements NoFilesCommand {
    @Override
    public boolean parse(String[] args) {
        if (args.length != 1)
            return false;

        return VERSION.contains(args[0]);
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, String[] args) {

        System.out.println("Insider " + InsiderConfiguration.getInstance().getInsiderVersion());
    }

    @Override
    public String usage() {
        return "insider {-v | -version | --version | version}";
    }
}
