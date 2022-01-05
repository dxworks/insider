package org.dxworks.insider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ChronosTag {
    private String entity;
    private String tag;
    private Integer strength;
}
