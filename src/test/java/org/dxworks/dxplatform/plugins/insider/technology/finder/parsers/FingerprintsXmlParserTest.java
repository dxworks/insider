package org.dxworks.dxplatform.plugins.insider.technology.finder.parsers;

import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class FingerprintsXmlParserTest {

    @Test
    public void parseString() throws JAXBException {
        FingerprintsXmlParser fingerprintsXmlParser = new FingerprintsXmlParser();

        fingerprintsXmlParser.parseString(Paths.get("C:\\Users\\Mario\\Desktop\\libraries.xml").toFile());
    }
}