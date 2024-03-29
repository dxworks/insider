package org.dxworks.insider.library.detector;

import lombok.extern.slf4j.Slf4j;
import org.dxworks.insider.InsiderAnalysis;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.InsiderResult;
import org.dxworks.insider.technology.finder.LinguistService;
import org.dxworks.insider.utils.FileUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

@Slf4j
public class LibraryDetector implements InsiderAnalysis {

    private static final String JAVA_LANGUAGE = "java";
    private static final List<String> C_LIKE_LANGUAGES = asList("c", "c++", "objective-c", "objective-c++");
    public static final List<String> ACCEPTED_LANGUAGES = Stream.concat(
            Stream.of(JAVA_LANGUAGE),
            C_LIKE_LANGUAGES.stream())
            .collect(Collectors.toList());

    private LinguistService linguistService = LinguistService.getInstance();
    private ImportsContainer importsContainer;
    private LibraryDetectorLanguage language;

    public LibraryDetector(LibraryDetectorLanguage language) {
        this.language = language;
        if (language == LibraryDetectorLanguage.C_LIKE)
            importsContainer = new C_ImportsContainer();
        if (language == LibraryDetectorLanguage.JAVA)
            importsContainer = new JavaImportsContainer();
    }

    public InsiderResult analyze(InsiderFile file) {
        int importNumber = 0;
        String content = file.getContent();

        content = FileUtils.removeComments(content);

        if (file.getExtension().equals("java") && !"package-info.java".equalsIgnoreCase(file.getName())) {
            importNumber = analyzeForJava(content, file.getPath());
        } else if (file.getExtension().equals("m") || file.getExtension().equals("mm") || file.getExtension()
                .equals("h") || file.getExtension().equals("cpp") || file.getExtension().equals("c")) {
            importNumber = analyzeForC_Like(content);
        }

        if (importNumber > 0) {
            importsContainer.addFile(file.getName(), importNumber);
        }

        return null;
    }

    private int analyzeForC_Like(String content) {
        int importNumber = 0;
        importNumber += getImportNumberForC_Like(content, "(#(include|import)[ \t]*<[^>]*>)", line ->
        {
            int firstIndex = line.indexOf('<');
            int lastIndex = line.indexOf('>');
            return line.substring(firstIndex + 1, lastIndex);
        });

        importNumber += getImportNumberForC_Like(content, "(#(include|import)[ \t]*\"[^\"]*\")", line ->
        {
            int firstIndex = line.indexOf('\"');
            int lastIndex = line.indexOf('\"', firstIndex + 1);
            return line.substring(firstIndex + 1, lastIndex);
        });

        return importNumber;
    }

    private int getImportNumberForC_Like(String content, String usedPattern,
                                         C_LikeImportExtractor cLikeImportExtractor) {
        int importNumber = 0;
        Pattern pattern = Pattern.compile(usedPattern);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String importLine = content.substring(matcher.start(), matcher.end());
            String importString = cLikeImportExtractor.getImportedHeaderFromLine(importLine);

            int index = importString.lastIndexOf('/');
            importString = importString.substring(++index);

            if (importsContainer.accepts(importString)) {
                importsContainer.addImport(importString);
                importNumber++;

            }
        }
        return importNumber;
    }

    private int analyzeForJava(String content, String filePath) {
        int firstAcc = content.indexOf("{");
        if (firstAcc == -1) {
            log.warn("Java file " + filePath + " has no class / viable content!");
            return 0;
        }
        content = content.substring(0, firstAcc);

        int importNumber = 0;

        Pattern pattern = Pattern.compile("(import [^;]*;)");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String importString = content.substring(matcher.start() + 7, matcher.end() - 1).trim();

            if (importString.indexOf("static ") == 0 || importString.indexOf("static\t") == 0) {
                importString = importString.substring(6).trim();
            }

            if (importsContainer.accepts(importString)) {
                importsContainer.addImport(importString);
                importNumber++;
            }
        }

        return importNumber;
    }

    @Override
    public boolean accepts(InsiderFile insiderFile) {
        if (language == LibraryDetectorLanguage.JAVA) {
            return linguistService.hasAcceptedExtension(insiderFile, List.of(JAVA_LANGUAGE));
        }

        if (language == LibraryDetectorLanguage.C_LIKE) {
            return linguistService.hasAcceptedExtension(insiderFile, C_LIKE_LANGUAGES);
        }

        return false;
    }

    public void generateResults() {
        importsContainer.writeFilesWithImports();
        importsContainer.writeImportsToFile();
        importsContainer.writePackagingUnits();
    }

    private interface C_LikeImportExtractor {
        String getImportedHeaderFromLine(String line);
    }
}
