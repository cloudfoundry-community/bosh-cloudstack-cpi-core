package com.orange.oss.cloudfoundry.cscpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.domain.ResourcePool;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
public class JsonParserTest {

    @Test
    public void it_parses_resourcepool_into_data_classes() throws IOException {
        //given
        String json = TestResourceLoader.loadLocalResource("resourcepool.json");

        //when
        ObjectMapper m = new ObjectMapper();
        ResourcePool resourcePool = m.readValue(json, ResourcePool.class);
        //then
        ResourcePool expectedPool = new ResourcePool("1024", "8192", 1);
        expectedPool.cpu=1;
        expectedPool.ram="1024";
        expectedPool.disk="8192";
        assertThat(resourcePool).isEqualTo(expectedPool);
    }

    @Test
    @Ignore
    public void it_parses_network_into_data_classes() throws IOException {
        //given
        String json = TestResourceLoader.loadLocalResource("resourcepool.json");

        //when
        ObjectMapper m = new ObjectMapper();
        ResourcePool resourcePool = m.readValue(json, ResourcePool.class);
        //then
        ResourcePool expectedPool = new ResourcePool("1024", "8192", 1);
        assertThat(resourcePool).isEqualTo(expectedPool);
    }

}
