package org.dxworks.insider.technology.finder.parsers;

import org.dxworks.insider.technology.finder.exceptions.FingerprintParseException;
import org.dxworks.insider.technology.finder.model.Technology;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FingerprintsParser {
    List<Technology> parseTechnologiesFile(String filePath) throws FingerprintParseException;

    void writeTechnologiesToFile(List<Technology> technologies, Path filePath) throws IOException;
}
