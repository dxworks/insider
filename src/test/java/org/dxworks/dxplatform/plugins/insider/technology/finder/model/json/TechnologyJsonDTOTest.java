package org.dxworks.dxplatform.plugins.insider.technology.finder.model.json;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TechnologyJsonDTOTest {

    private TechnologyJsonDTO technology;

    @Before
    public void setup() {
        technology = new TechnologyJsonDTO();
    }

    @Test
    public void fingerprintsWrappedAsImports() {
        String expected = "(org\\.dxworks\\.dxplatform)([\\.;])([a-zA-Z_0-9]*\\.)*([a-zA-Z_0-9]*|\\*)*(;){0,1}";
        String actual = technology.convertToRegularExpression("org.dxworks.dxplatform");

        assertEquals(expected, actual);
    }

    @Test
    public void testUnwrapImport() {
        String expected = "org.dxworks.dxplatform";
        String actual = TechnologyJsonDTO.unwrapImport("(org\\.dxworks\\.dxplatform)([\\.;])([a-zA-Z_0-9]*\\.)*([a-zA-Z_0-9]*|\\*)*(;){0,1}");

        assertEquals(expected, actual);
    }
}