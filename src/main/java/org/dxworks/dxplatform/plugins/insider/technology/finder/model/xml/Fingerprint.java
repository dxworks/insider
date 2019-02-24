package org.dxworks.dxplatform.plugins.insider.technology.finder.model.xml;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement(name = "fingerprint")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Fingerprint {

    @XmlValue
    private String value;
}
