package org.dxworks.insider.application.inspector.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static java.util.regex.Pattern.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public
class CommentPattern {
    private String pattern;
    private List<String> modifiers;

    @JsonIgnore
    private int regexFlags = -1;

    public int createModifier() {
        if (regexFlags != -1)
            return regexFlags;

        regexFlags = 0;

        if (modifiers == null)
            modifiers = new ArrayList<>();

        if (modifiers.contains("i"))
            regexFlags = regexFlags | CASE_INSENSITIVE;

        if (modifiers.contains("d"))
            regexFlags = regexFlags | DOTALL;

        if (modifiers.contains("m"))
            regexFlags = regexFlags | MULTILINE;

        return regexFlags;
    }
}