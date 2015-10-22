package com.orange.oss.cloudfoundry.cscpi.logic;

import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.NetworkType;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;


public class UserDataGeneratorImplTest {
    
    @Test
    public void generates_user_data_expected_by_bosh_agent_ie_server_name_registry_and_dns() {
        //given
        UserDataGeneratorImpl userDataGenerator = new UserDataGeneratorImpl();
        userDataGenerator.endpoint = "http://192.168.0.255:8080/client/api";

        Networks networks = aSampleNetworksBlock();

        //when
        String userMetadata = userDataGenerator.userMetadata("vm-384sd4-r7re9e", networks);

        //then
        assertThat(userMetadata).isEqualTo(
                "{\"server\":{\"name\":\"vm-384sd4-r7re9e\"}," +
                        "\"registry\":{\"endpoint\":\"http://192.168.0.255:8080/client/api\"}," +
                        "\"dns\":{\"nameserver\":[\"10.234.50.180\"]}}");
    }

    private Networks aSampleNetworksBlock() {
        Networks networks=new Networks();
        Network net=new Network();
        networks.networks.put("default", net);
        //net.type=NetworkType.dynamic;
        net.type= NetworkType.manual;
        net.ip="10.234.228.155";
        net.gateway="10.234.228.129";
        net.netmask="255.255.255.192";
        net.cloud_properties.put("name", "3112 - preprod - back");
        net.dns.add("10.234.50.180");
        net.dns.add("10.234.71.124");
        return networks;
    }

}