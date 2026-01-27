package org.dxworks.insider.depext;

public class CsvUtils {
    
    public static String escapeCsvValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
