package org.dxworks.dxplatform.plugins.insider.technology.finder.parsers;

import org.dxworks.dxplatform.plugins.insider.technology.finder.model.Technology;
import org.dxworks.dxplatform.plugins.insider.technology.finder.model.xml.old.FingerprintDTO;
import org.dxworks.dxplatform.plugins.insider.technology.finder.model.xml.old.XmlConfigurationDTO;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FingerprintsXmlParser implements FingerprintsParser {

    public List<String> parseString(File fingerPrintsFile) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(XmlConfigurationDTO.class);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        XmlConfigurationDTO xmlConfiguration = (XmlConfigurationDTO) unmarshaller.unmarshal(fingerPrintsFile);

        return xmlConfiguration.getData().getLanguages().getLanguages().stream()
                .flatMap(language -> language.getTechnologies().stream()
                        .flatMap(technology -> technology.getCategories().stream()
                                .flatMap(category -> category.getFingerprints().stream().map(FingerprintDTO::getValue))))
                .collect(Collectors.toList());
    }

    @Override
    public List<Technology> parseTechnologiesFile(String filePath) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(XmlConfigurationDTO.class);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        XmlConfigurationDTO xmlConfiguration = (XmlConfigurationDTO) unmarshaller.unmarshal(Paths.get(filePath).toFile());

        return xmlConfiguration.getData().getLanguages().getLanguages().stream()
                .flatMap(languageDTO ->
                        languageDTO.getTechnologies().stream()
                                .map(technologyDTO -> Technology.builder()
                                        .languages(Collections.singletonList(languageDTO.getName()))
                                        .extensions(Collections.emptyList())
                                        .build()))
                .collect(Collectors.toList());
    }
}
