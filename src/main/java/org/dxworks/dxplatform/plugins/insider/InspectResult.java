package org.dxworks.dxplatform.plugins.insider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class InspectResult {
    private String entity;
    private String tag;
    private Integer strength;
}