package org.dxworks.insider.commands;

import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.configuration.InsiderConfiguration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelpCommand implements NoFilesCommand {
    @Override
    public boolean parse(List<String> args) {
        if (args.size() != 1)
            return false;

        return HELP.contains(args.get(0));
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, List<String> args) {
        String usage = "Insider " + InsiderConfiguration.getInstance().getInsiderVersion() + " -  usage guide:\n";
        usage += "Configure the source root and the project id in the config/insider-conf.properties file\n\n";

        usage += "This is a list of the commands:\n";

        usage += Stream.of(
                        new HelpCommand(),
                        new VersionCommand(),
                        new AddCommand(),
                        new ConvertCommand(),
                        new DiagnoseCommand(),
                        new DetectCommand(),
                        new FindCommand(),
                        new InspectCommand(),
                        new ExtractCommand(),
                        new MeasureCommand(),
                        new IndentationCount(),
                        new ClocCommand())
                .map(InsiderCommand::usage)
                .map(s -> "\t" + s)
                .collect(Collectors.joining("\n"));

        usage += "\n\nPlease run insider with the specified commands from the folder you have installed Insider to!\n";

        System.out.println(usage);
    }

    @Override
    public String usage() {
        return "insider {-h | -help | --help | help}";
    }

    @Override
    public String getName() {
        return "help";
    }
}
