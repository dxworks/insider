package org.dxworks.dxplatform.plugins.insider.technology.finder.model.json;

import lombok.Data;
import org.dxworks.dxplatform.plugins.insider.technology.finder.model.Technology;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Data
public class TechnologyJsonDTO {

    private static final String IMPORT_SUFFIX = "([\\.;])([a-zA-Z_0-9]*\\.)*([a-zA-Z_0-9]*|\\*)*(;){0,1}";
    private String category;
    private String name;
    private List<String> languages;
    private List<String> extensions;
    private List<String> fingerprints;

    private boolean wrapAsImports;

    public static TechnologyJsonDTO fromTechnology(Technology technology) {
        TechnologyJsonDTO technologyJsonDTO = new TechnologyJsonDTO();

        technologyJsonDTO.setName(technology.getName());
        technologyJsonDTO.setCategory(technology.getCategory());
        technologyJsonDTO.setExtensions(technology.getExtensions());
        technologyJsonDTO.setLanguages(technology.getLanguages());

        AtomicBoolean shouldWrapAsImports = new AtomicBoolean(false);

        List<String> fingerPrints = technology.getFingerprints().stream().map(fingerPrint -> {
            if (fingerPrint.contains(IMPORT_SUFFIX)) {
                shouldWrapAsImports.set(true);
                return unwrapImport(fingerPrint);
            }
            return fingerPrint;
        }).collect(Collectors.toList());

        technologyJsonDTO.setFingerprints(fingerPrints);
        technologyJsonDTO.setWrapAsImports(shouldWrapAsImports.get());

        return technologyJsonDTO;
    }

    protected static String unwrapImport(String wrappedImport) {
        int index = wrappedImport.indexOf(IMPORT_SUFFIX);
        if (index == -1)
            return wrappedImport;

        String _import = wrappedImport.substring(1, index - 1);
        _import = _import.replace("\\", "");

        return _import;
    }

    public Technology toTechnology() {
        Technology technology = new Technology();

        technology.setName(name);
        technology.setCategory(category);
        technology.setLanguages(languages != null ? languages : Collections.emptyList());
        technology.setExtensions(extensions != null ? extensions : Collections.emptyList());
        technology.setFingerprints(fingerprints == null ? Collections.emptyList() : wrapAsImports ? fingerprintsWrappedAsImports() : fingerprints);

        return technology;
    }

    protected List<String> fingerprintsWrappedAsImports() {
        return fingerprints.stream().map(this::convertToRegularExpression).collect(Collectors.toList());
    }

    protected String convertToRegularExpression(String _import) {
        return "(" + _import.replace(".", "\\.") + ")" + IMPORT_SUFFIX;
    }

}
