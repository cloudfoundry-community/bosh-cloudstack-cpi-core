package com.orange.oss.cloudfoundry.cscpi;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.NetworkType;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import com.orange.oss.cloudfoundry.cscpi.domain.ResourcePool;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
@WebIntegrationTest
@ConfigurationProperties
public class CPIFlowTest {
	
	@Autowired
	CPI cpi;
	
	
	/**
	 * see reference doc
	 * @see https://github.com/cloudfoundry/bosh-init/blob/master/docs/architecture.md
	 */
	@Test
	public void testCompleteFlow(){
		//provided by bosh ?
		String agent_id="xxxxx";
		
		String image_path="/tmp/image.vhd";
		Map<String, String> cloud_properties=new HashMap<String, String>();
		String stemcell_id=cpi.create_stemcell(image_path, cloud_properties);

		
		ResourcePool resource_pool=new ResourcePool();
		resource_pool.compute_offering="CO1 - Small STD";
		resource_pool.disk=8192;
		resource_pool.ephemeral_disk_offering="custom_size_disk_offering";
		
		Networks networks=new Networks();
		Network net=new Network();
		networks.networks.put("default", net);
		net.type=NetworkType.manual;
		net.ip="10.234.229.30";
		net.gateway="10.234.229.1";
		net.netmask="255.255.255.192";
		net.cloud_properties.put("name", "3113 - prod - back");

		List<String> disk_locality=new ArrayList<String>();
		Map<String, String> env=new HashMap<String, String>();

		String vm_id=cpi.create_vm(agent_id, stemcell_id, resource_pool, networks, disk_locality, env);
		
		Integer size=new Integer(10);		
		Map<String, String> diskcloud_properties=new HashMap<String, String>();
		String disk_id=cpi.create_disk(size, diskcloud_properties);
		
		//TODO assert disk
		cpi.attach_disk(vm_id, disk_id);


		//reboot the vm
		cpi.reboot_vm(vm_id);
		
		
		//delete flow

		//disk include root disk, so 2 disks + 1 ephemeral disk : 3		
		List<String >disks=cpi.get_disks(vm_id);
		assertEquals(3, disks.size());

		
		cpi.detach_disk(vm_id, disk_id);

		disks=cpi.get_disks(vm_id);
		assertEquals(2, disks.size());		
		
		
		//TODO assert hasVM
//		boolean exist=cpi.has_vm(vm_id);
		
		cpi.delete_vm(vm_id);
		cpi.delete_disk(disk_id);
		
	}

}
