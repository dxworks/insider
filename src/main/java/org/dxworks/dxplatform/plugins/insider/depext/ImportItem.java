package org.dxworks.dxplatform.plugins.insider.depext;

public class ImportItem {
    String name;
    String attribute;

    public ImportItem(String _name, String _attribute) {
        name = _name;
        attribute = _attribute;
    }

    public ImportItem(String _name) {
        this(_name, "");
    }

    public String toString() {
        return attribute.isEmpty() ? name : name + "," + attribute;
    }
}
