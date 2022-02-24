package org.dxworks.insider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InsiderResult {
    private String name;
    private String category;
    private String file;
    private Integer value;
}
