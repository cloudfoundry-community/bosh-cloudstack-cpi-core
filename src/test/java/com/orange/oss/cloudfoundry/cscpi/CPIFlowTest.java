package com.orange.oss.cloudfoundry.cscpi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
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
		
		
		
		String image_path="/tmp";
		Map<String, String> cloud_properties=new HashMap<String, String>();
		String stemcell_id=cpi.create_stemcell(image_path, cloud_properties);

		
		JsonNode resource_pool=null;
		JsonNode networks=null;		
		List<String> disk_locality=new ArrayList<String>();
		Map<String, String> env=new HashMap<String, String>();
		

		String vm_id=cpi.create_vm(agent_id, stemcell_id, resource_pool, networks, disk_locality, env);
		
		
		
		
		Integer size=new Integer(10);		
		Map<String, String> diskcloud_properties=new HashMap<String, String>();
		String disk_id=cpi.create_disk(size, diskcloud_properties);
		
		//TODO assert disk
		
		cpi.attach_disk(vm_id, disk_id);

		
		//delete flow
		
		List<String >disks=cpi.get_disks(vm_id);
		assertEquals(1, disks.size());
		
		
		cpi.detach_disk(vm_id, disk_id);

		//TODO assert hasVM
//		boolean exist=cpi.has_vm(vm_id);

		
		cpi.delete_vm(vm_id);
		cpi.delete_disk(disk_id);
		
	}

}