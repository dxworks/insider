package org.dxworks.dxplatform.plugins.insider.technology.finder.model.xml;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "language")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Language {
    private String name;
    private int version;

    @XmlElement(name = "technology")
    private List<Technology> technologies;
}
