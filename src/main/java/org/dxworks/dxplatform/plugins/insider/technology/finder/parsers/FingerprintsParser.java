package org.dxworks.dxplatform.plugins.insider.technology.finder.parsers;

import org.dxworks.dxplatform.plugins.insider.technology.finder.model.Technology;

import javax.xml.bind.JAXBException;
import java.util.List;

public interface FingerprintsParser {
    List<Technology> parseTechnologiesFile(String filePath) throws JAXBException;
}
