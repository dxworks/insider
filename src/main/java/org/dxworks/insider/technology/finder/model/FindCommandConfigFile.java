package org.dxworks.insider.technology.finder.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FindCommandConfigFile {
    private List<Technology> technologies;
    private String outputFile;
}