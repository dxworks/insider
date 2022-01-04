package org.dxworks.dxplatform.plugins.insider.application.inspector.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private List<String> languages;
    private String inline;
    private String preffix;
    private String suffix;
}
