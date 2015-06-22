package com.orange.oss.cloudfoundry.cscpi.explorejson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orange.oss.cloudfoundry.cscpi.TestResourceLoader;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
public class JsonToGenericCpiInterface {
    
    @Test
    public void it_extracts_() {
        
    }

    @Test
    public void it_parses_resourcepool_into_generic_jsonnode() throws IOException {
        //given
        String json = TestResourceLoader.loadLocalResource("resourcepool-with-units.json");

        //when
        ObjectMapper m = new ObjectMapper();
        JsonNode resourcePool = m.readValue(json, JsonNode.class);

        int cpu = resourcePool.get("cpu").asInt();
        String ram = resourcePool.get("ram").asText();
        String disk = resourcePool.get("disk").asText();

        assertThat(cpu).isEqualTo(1);
        assertThat(disk).isEqualTo("20_000");
        assertThat(ram).isEqualTo("1024m");
    }

    @Test
    public void it_parses_network_into_generic_structures() throws IOException {
        //given
        String json = TestResourceLoader.loadLocalResource("networks.json");

        //when
        ObjectMapper m = new ObjectMapper();
        JsonNode networks = m.readValue(json, JsonNode.class);

        ObjectNode defaultNetwork = (ObjectNode) networks.get("default");
        assertThat(defaultNetwork).isNotNull();

        assertThat(defaultNetwork.get("ip").asText()).isEqualTo("10.203.7.0");
        assertThat(defaultNetwork.get("netmask").asText()).isEqualTo("255.255.254.0");

        assertThat(defaultNetwork.get("cloud_properties").isObject()).isTrue();
        ObjectNode cloud_properties = (ObjectNode) defaultNetwork.get("cloud_properties");
        assertThat(cloud_properties.get("name").asText()).isEqualTo("NET_APPS_SERV");

        ArrayNode defaultNetworkServices = (ArrayNode) defaultNetwork.get("default");
        for (Iterator<JsonNode> iter = defaultNetworkServices.iterator(); iter.hasNext(); ) {
            JsonNode service = iter.next();
            assertThat(service.isTextual());
            assertThat(service.asText()).isIn(asList("dns", "gateway"));
        }
    }

}
