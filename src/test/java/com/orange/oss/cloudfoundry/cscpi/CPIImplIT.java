package com.orange.oss.cloudfoundry.cscpi;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import com.orange.oss.cloudfoundry.cscpi.domain.ResourcePool;
import com.orange.oss.cloudfoundry.cscpi.exceptions.CpiErrorException;
import com.orange.oss.cloudfoundry.cscpi.exceptions.VMCreationFailedException;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
@ConfigurationProperties

public class CPIImplIT {

	
	@Autowired
	CPI cpi;
	

	
	
	@Test
	public void testCreate_vm() throws VMCreationFailedException, CpiErrorException {
		//should be template Id
		String agent_id="123456789";
		
		//TODO: add stemcell generation step = template creation
		//String stemcell_id="Ubuntu Trusty amd64 [2015-06-01]"; //ubuntu precise template";
		String stemcell_id="cpitemplate-601";
		
		ResourcePool resource_pool=new ResourcePool();
		resource_pool.compute_offering="CO1 - Small STD";
		resource_pool.disk=8192;
		
		Networks networks=new Networks();
		Network net=new Network();
		networks.networks.put("default", net);		
		net.ip="192.168.0.1";
		net.gateway="192.168.0.254";
		net.netmask="255.255.255.0";
		net.cloud_properties.put("", "");
		List<String> disk_locality=new ArrayList<String>();
		Map<String, String> env=new HashMap<String, String>();
		

		String vm_id=cpi.create_vm(agent_id, stemcell_id, resource_pool, networks, disk_locality, env);
		
		//clean
		cpi.delete_vm(vm_id);
	}


	@Test
	public void testCurrent_vm_id() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreate_stemcell() throws CpiErrorException {
		
		Map<String, String> cloud_properties=new HashMap<String, String>();
		String image_path="/tmp/image.vhd";
		String stemcell=this.cpi.create_stemcell(image_path, cloud_properties);
		//TODO: clean stemcell
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
		
		Integer size=new Integer(10);		
		Map<String, String> cloud_properties=new HashMap<String, String>();
		String disk_id=cpi.create_disk(size, cloud_properties);
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

}
