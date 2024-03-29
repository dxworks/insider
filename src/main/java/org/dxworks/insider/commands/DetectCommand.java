package org.dxworks.insider.commands;

import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.library.detector.LibraryDetector;
import org.dxworks.insider.library.detector.LibraryDetectorLanguage;
import org.dxworks.insider.technology.finder.LinguistService;

import java.util.List;

public class DetectCommand implements InsiderCommand {

    @Override
    public boolean parse(List<String> args) {
        return args.size() == 1;
    }


    @Override
    public void execute(List<InsiderFile> insiderFiles, List<String> args) {
        LibraryDetector libraryDetector = new LibraryDetector(LibraryDetectorLanguage.JAVA);
        insiderFiles.stream()
                .filter(libraryDetector::accepts)
                .forEach(libraryDetector::analyze);

        libraryDetector.generateResults();
    }

    @Override
    public String usage() {
        return "insider detect";
    }

    @Override
    public String getName() {
        return DETECT;
    }

    @Override
    public boolean acceptsFile(String path) {
        return LinguistService.getInstance().hasAcceptedExtension(path, LibraryDetector.ACCEPTED_LANGUAGES);
    }
}
