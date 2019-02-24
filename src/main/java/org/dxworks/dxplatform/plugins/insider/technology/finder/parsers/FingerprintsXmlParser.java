package org.dxworks.dxplatform.plugins.insider.technology.finder.parsers;

import org.dxworks.dxplatform.plugins.insider.technology.finder.model.xml.Fingerprint;
import org.dxworks.dxplatform.plugins.insider.technology.finder.model.xml.XmlConfiguration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class FingerprintsXmlParser implements FingerprintsParser {

    public List<String> parseString(File fingerPrintsFile) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(XmlConfiguration.class);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        XmlConfiguration xmlConfiguration = (XmlConfiguration) unmarshaller.unmarshal(fingerPrintsFile);

        return xmlConfiguration.getData().getLanguages().getLanguages().stream()
                .flatMap(language -> language.getTechnologies().stream()
                        .flatMap(technology -> technology.getCategories().stream()
                                .flatMap(category -> category.getFingerprints().stream().map(Fingerprint::getValue))))
                .collect(Collectors.toList());
    }
}
