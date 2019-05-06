package org.dxworks.dxplatform.plugins.insider.library.detector;

import lombok.extern.slf4j.Slf4j;
import org.dxworks.dxplatform.plugins.insider.library.detector.model.PackagingUnit;
import org.dxworks.dxplatform.plugins.insider.utils.MapUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class ImportsContainer {

    protected Map<String, Integer> filesToNumberOfImportsMap = new HashMap<>();
    protected Map<String, Integer> importedFilesToNumberOfTimesBeingImportedMap = new HashMap<>();

    public ImportsContainer() {
        configure();
    }

    protected abstract void configure();

    public void addImport(String newImport) {
        int newapp = 1;
        Integer app = importedFilesToNumberOfTimesBeingImportedMap.get(newImport);
        if (app != null)
            newapp = app + 1;
        importedFilesToNumberOfTimesBeingImportedMap.put(newImport, newapp);
    }

    public void addFile(String fileName, int importNo) {
        filesToNumberOfImportsMap.put(fileName, importNo);
    }

    public void writeImportsToFile() {
        importedFilesToNumberOfTimesBeingImportedMap = MapUtils.sortMapByValuesDesc(importedFilesToNumberOfTimesBeingImportedMap);

        String content = getContentFromMap(importedFilesToNumberOfTimesBeingImportedMap);

        try {
            Files.write(Paths.get(getImportsResultFilePath()), content.getBytes());
        } catch (IOException e) {
            log.error("Could not write Imports file!", e);
        }
    }

    private String getContentFromMap(Map<String, Integer> map) {
        return map.entrySet().stream()
                .map(entry -> String.join("\t", entry.getKey(), entry.getValue().toString()))
                .collect(Collectors.joining("\n"));
    }

    protected abstract String getImportsResultFilePath();

    public void writeFilesWithImports() {
        filesToNumberOfImportsMap = MapUtils.sortMapByValuesDesc(filesToNumberOfImportsMap);

        String content = getContentFromMap(filesToNumberOfImportsMap);

        try {
            Files.write(Paths.get(getFilesWithImportsResultFile()), content.getBytes());
        } catch (IOException e) {
            log.error("Could not write Imports file!", e);
        }
    }

    protected abstract String getFilesWithImportsResultFile();

    public void writePackagingUnits() {
        List<PackagingUnit> packagingUnitList = createPackagingUnitList();
        packagingUnitList = packagingUnitList.stream()
                .sorted(Comparator.comparing(PackagingUnit::getFrequency).reversed())
                .collect(Collectors.toList());

        String content = packagingUnitList.stream()
                .map(packagingUnit -> String.join("\t",
                        packagingUnit.getName(),
                        packagingUnit.getFrequency() + "",
                        packagingUnit.getUnitNumber() + ""))
                .collect(Collectors.joining("\n"));

        try {
            Files.write(Paths.get(getPackagingUnitResultFilePath()), content.getBytes());
        } catch (IOException e) {
            log.error("Could not write packaging unit result file file!", e);
        }
    }

    protected abstract String getPackagingUnitResultFilePath();

    public abstract boolean accepts(String importString);

    protected List<PackagingUnit> createPackagingUnitList() {
        List<PackagingUnit> packagingUnits = new ArrayList<>();

        for (String importedFile : importedFilesToNumberOfTimesBeingImportedMap.keySet()) {
            String packagingUnitName = getPackagingUnitFor(importedFile);
            Integer frequency = importedFilesToNumberOfTimesBeingImportedMap.get(importedFile);

            PackagingUnit packagingUnit = packagingUnits.stream().filter(packUnit -> packUnit.getName().equals(packagingUnitName)).findFirst().orElse(null);
            if (packagingUnit == null) {
                packagingUnit = new PackagingUnit(packagingUnitName, frequency);
                packagingUnits.add(packagingUnit);
            } else {
                packagingUnit.increment(frequency);
            }
        }
        return packagingUnits;
    }

    protected abstract String getPackagingUnitFor(String importedFile);
}
