package org.dxworks.dxplatform.plugins.insider.technology.finder.parsers;

import org.dxworks.dxplatform.plugins.insider.technology.finder.exceptions.FingerprintParseException;
import org.dxworks.dxplatform.plugins.insider.technology.finder.model.Technology;
import org.dxworks.dxplatform.plugins.insider.technology.finder.model.xml.old.XmlConfigurationDTO;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FingerprintsXmlParser implements FingerprintsParser {

    @Override
    public List<Technology> parseTechnologiesFile(String filePath) throws FingerprintParseException {
        Unmarshaller unmarshaller;
        XmlConfigurationDTO xmlConfiguration;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(XmlConfigurationDTO.class);
            unmarshaller = jaxbContext.createUnmarshaller();
            xmlConfiguration = (XmlConfigurationDTO) unmarshaller.unmarshal(Paths.get(filePath).toFile());

        } catch (JAXBException e) {
            throw new FingerprintParseException(e);
        }

        return xmlConfiguration.getData().getLanguages().getLanguages().stream()
                .flatMap(languageDTO -> languageDTO.getTechnologies().stream()
                        .flatMap(technologyDTO -> technologyDTO.getCategories().stream().map(categoryDTO -> {

                            Technology technology = new Technology();
                            technology.setLanguages(Collections.singletonList(languageDTO.getName()));
                            technology.setExtensions(Collections.emptyList());
                            technology.setCategory(technologyDTO.getName());
                            technology.setName(categoryDTO.getName());

                            technology.setFingerprints(categoryDTO.getFingerprints().stream()
                                    .map(fingerprintXmlDTO -> fingerprintXmlDTO.getValue())
                                    .collect(Collectors.toList()));

                            return technology;
                        })))
                .collect(Collectors.toList());
    }
}
