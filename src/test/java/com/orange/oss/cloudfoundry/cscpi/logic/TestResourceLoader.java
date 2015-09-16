package com.orange.oss.cloudfoundry.cscpi.logic;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 *
 */
public class TestResourceLoader {
    public static String loadLocalResource(String jsonFileName) throws IOException {
        return IOUtils.toString(TestResourceLoader.class.getClassLoader().getResourceAsStream(jsonFileName));
    }
}
