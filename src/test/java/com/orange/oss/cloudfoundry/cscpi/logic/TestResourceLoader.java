package com.orange.oss.cloudfoundry.cscpi.logic;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

/**
 *
 */
public class TestResourceLoader {
    public static String loadLocalResource(String jsonFileName) throws IOException {
        return IOUtils.toString(TestResourceLoader.class.getClassLoader().getResourceAsStream(jsonFileName));
    }
}
