package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Language {
    private List<String> extensions;
    private String name;
    private String type;
}