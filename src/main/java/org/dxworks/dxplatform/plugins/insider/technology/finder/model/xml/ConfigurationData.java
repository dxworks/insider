package org.dxworks.dxplatform.plugins.insider.technology.finder.model.xml;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "data")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class ConfigurationData {

    @XmlElement(name = "languages")
    private Languages languages;
}
