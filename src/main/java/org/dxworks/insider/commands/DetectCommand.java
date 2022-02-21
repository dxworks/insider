package org.dxworks.insider.commands;

import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.library.detector.LibraryDetector;
import org.dxworks.insider.library.detector.LibraryDetectorLanguage;
import org.dxworks.insider.technology.finder.LinguistService;

import java.util.List;

public class DetectCommand implements FilesCommand {

    private LibraryDetector libraryDetector = new LibraryDetector(LibraryDetectorLanguage.JAVA);

    @Override
    public boolean parse(List<String> args) {
        return args.size() == 1;
    }


    @Override
    public void init(List<String> args) {
    }

    @Override
    public void analyse(InsiderFile file) {
        if(libraryDetector.accepts(file))
            libraryDetector.analyze(file);
    }

    @Override
    public void writeResults() {
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
