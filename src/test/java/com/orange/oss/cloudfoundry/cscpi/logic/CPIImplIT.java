package com.orange.oss.cloudfoundry.cscpi.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import com.orange.oss.cloudfoundry.cscpi.domain.ResourcePool;
import com.orange.oss.cloudfoundry.cscpi.domain.VMParams;
import com.orange.oss.cloudfoundry.cscpi.exceptions.CpiErrorException;
import com.orange.oss.cloudfoundry.cscpi.exceptions.VMCreationFailedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ConfigurationProperties
public class CPIImplIT {

    private static Logger logger = LoggerFactory.getLogger(CPIImplIT.class.getName());

    @Autowired
    CPI cpi;


    @Test
    public void testCreate_vm() throws VMCreationFailedException, CpiErrorException, JsonProcessingException, IOException {
        //should be template Id
        String agent_id = "123456789";

        //TODO: add stemcell generation step = template creation
        //String stemcell_id="Ubuntu Trusty amd64 [2015-06-01]"; //ubuntu precise template";
        String stemcell_id = "cpitemplate-601";

        ResourcePool resource_pool = new ResourcePool();
        resource_pool.compute_offering = "CO1 - Small STD";
        resource_pool.disk = 8192;

        Networks networks = new Networks();
        Network net = new Network();
        networks.networks.put("default", net);
        net.ip = "192.168.0.1";
        net.gateway = "192.168.0.254";
        net.netmask = "255.255.255.0";
        net.cloud_properties.put("", "");
        List<String> disk_locality = new ArrayList<String>();
        String envString = "{\"bosh\":{\"password\":\"zzzzzz\"}}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode env = mapper.readTree(envString);

        String vm_id = cpi.create_vm(agent_id, stemcell_id, resource_pool, networks, disk_locality, env);

        //clean
        cpi.delete_vm(vm_id);
    }


    @Test
    public void testCurrent_vm_id() {
        fail("Not yet implemented");
    }

    @Test
    public void testCreate_stemcell() throws CpiErrorException, IOException {

        String current = new java.io.File(".").getCanonicalPath();
        logger.info("Current dir:" + current);

//    		cloud_properties:
//			  name: bosh-cloudstack-xen-ubuntu-trusty-go_agent
//			  version: '3033'
//			  infrastructure: cloudstack
//			  hypervisor: xen
//			  disk: 3072
//			  disk_format: raw
//			  container_format: bare
//			  os_type: linux
//			  os_distro: ubuntu
//			  architecture: x86_64
//			  auto_disk_config: true

        Map<String, Object> cloud_properties = new HashMap<String, Object>();
        cloud_properties.put("name", "light-bosh-cloudstack-xen-ubuntu-trusty-go_agent");
        cloud_properties.put("version", "3033");
        cloud_properties.put("infrastructure", "cloudstack");
        cloud_properties.put("hypervisor", "xen");
        cloud_properties.put("disk", 3072);
        cloud_properties.put("disk_format", "raw");
        cloud_properties.put("container_format", "bare");
        cloud_properties.put("os_type", "linux");
        cloud_properties.put("os_distro", "ubuntu");
        cloud_properties.put("architecture", "x86_64");
        cloud_properties.put("auto_disk_config", "true");

        //set template for light stemcell
        cloud_properties.put("light_template", "bosh-stemcell-3033-po10.vhd.bz2");

        String image_path = current + "/src/test/resources/mock-template.vhd";
        String stemcell = this.cpi.create_stemcell(image_path, cloud_properties);
        //TODO: clean stemcell
        this.cpi.delete_stemcell(stemcell);

    }

    @Test
    public void testDelete_stemcell() {
        fail("Not yet implemented");
    }

    @Test
    public void testDelete_vm() {
        fail("Not yet implemented");
    }

    @Test
    public void testHas_vm() {
        fail("Not yet implemented");
    }

    @Test
    public void testHas_disk() {
        fail("Not yet implemented");
    }

    @Test
    public void testReboot_vm() {
        fail("Not yet implemented");
    }

    @Test
    public void testSet_vm_metadata() {
        fail("Not yet implemented");
    }

    @Test
    public void testConfigure_networks() {
        fail("Not yet implemented");
    }

    @Test
    public void testCreate_disk() {

        Integer size = new Integer(10);
        Map<String, String> cloud_properties = new HashMap<String, String>();
        String disk_id = cpi.create_disk(size, cloud_properties);
        cpi.delete_disk(disk_id);

    }

    @Test
    public void testAttach_disk() {
        fail("Not yet implemented");
    }

    @Test
    public void testSnapshot_disk() {
        fail("Not yet implemented");
    }

    @Test
    public void testDelete_snapshot() {
        fail("Not yet implemented");
    }

    @Test
    public void testDetach_disk() {
        fail("Not yet implemented");
    }

    @Test
    public void testGet_disks() {
        fail("Not yet implemented");
    }

    @Test
    public void testCalculate_vm_cloud_properties() {
        VMParams vmParams = new VMParams();
        vmParams.ram = 8000;
        vmParams.cpu = 2;
        vmParams.ephemeralDiskSize = 50000;
        ResourcePool rp = this.cpi.calculate_vm_cloud_properties(vmParams);

        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(System.out, rp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
