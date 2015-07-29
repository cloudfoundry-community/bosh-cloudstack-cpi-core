package com.orange.oss.cloudfoundry.cscpi.explorejson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.TestResourceLoader;
import com.orange.oss.cloudfoundry.cscpi.domain.CPIRequest;
import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
public class JsonToCpiSpecificClasses {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void it_extracts_parts_from_tree_model_into_object_model() throws IOException {
        //given
        String json = TestResourceLoader.loadLocalResource("reference/createvm.json");
        CPIRequest cpiRequest = objectMapper.readValue(json, CPIRequest.class);

        //when
        JsonNode resourcePoolJson = cpiRequest.arguments.get(2);
        StringBuilderWriter out = new StringBuilderWriter();
        objectMapper.writeValue(out, resourcePoolJson);

        VSphereResourcePool resourcePool = objectMapper.readValue(new StringReader(out.toString()), VSphereResourcePool.class);

        //then
        VSphereResourcePool expectedPool = new VSphereResourcePool("1024", "8192", 1);
        expectedPool.cpu=1;
        expectedPool.ram="1024";
        expectedPool.disk="8192";
        assertThat(resourcePool).isEqualTo(expectedPool);
    }

    @Test
    public void it_parses_resourcepool_into_vsphere_specific_classes() throws IOException {
        //given
        String json = TestResourceLoader.loadLocalResource("resourcepool.json");

        //when
        VSphereResourcePool resourcePool = objectMapper.readValue(json, VSphereResourcePool.class);
        //then
        VSphereResourcePool expectedPool = new VSphereResourcePool("1024", "8192", 1);
        expectedPool.cpu=1;
        expectedPool.ram="1024";
        expectedPool.disk="8192";
        assertThat(resourcePool).isEqualTo(expectedPool);
    }

    @Test
    public void it_parses_network_into_vsphere_specific_classes() throws IOException {
        //given
        String json = TestResourceLoader.loadLocalResource("network.json");

        Network expected = new Network();
        expected.ip = "10.203.7.0";
        expected.netmask = "255.255.254.0";
        expected.dns = asList("10.203.6.105");
        expected.gateway = "10.203.7.254";
        expected.cloud_properties = new HashMap<>();
        expected.cloud_properties.put("name", "NET_APPS_SERV");
        expected.defaultServices = asList("dns", "gateway");


        //when
        Network network = objectMapper.readValue(json, Network.class);

        //then
        assertThat(network).isEqualTo(expected);

    }

}
