package com.orange.oss.cloudfoundry.cscpi.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.Zone;
import org.jclouds.cloudstack.options.ListZonesOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import com.orange.oss.cloudfoundry.cscpi.BoshCloudstackCpiCoreApplication;
import com.orange.oss.cloudfoundry.cscpi.config.CloudStackConfiguration;
import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.NetworkType;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import com.orange.oss.cloudfoundry.cscpi.domain.ResourcePool;
import com.orange.oss.cloudfoundry.cscpi.exceptions.CpiErrorException;
import com.orange.oss.cloudfoundry.cscpi.exceptions.VMCreationFailedException;
import com.orange.oss.cloudfoundry.cscpi.logic.CPI;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
@WebIntegrationTest
@ConfigurationProperties
public class CPIFlowIT {
	
	@Autowired
	CPI cpi;
	
	@Autowired
	CloudStackApi api;

	@Autowired
	CloudStackConfiguration cloudstackConfig;

	
	/**
	 * see reference doc
	 * @throws VMCreationFailedException 
	 * @throws CpiErrorException 
	 * @see https://github.com/cloudfoundry/bosh-init/blob/master/docs/architecture.md
	 */
	@Test
	public void testCompleteFlow() throws VMCreationFailedException, CpiErrorException{
		//provided by bosh ?
		String agent_id="xxxxx";
		
		String image_path="/tmp/image.vhd";
		Map<String, Object> cloud_properties=new HashMap<String, Object>();
		
		
		//simulate light stemcell provisionning from director
		
		cloud_properties.put("name","bosh-cloudstack-xen-ubuntu-trusty-go_agent");
		
		cloud_properties.put("version","3033");
		cloud_properties.put("infrastructure","cloudstack");
		cloud_properties.put("hypervisor","xen");		
		cloud_properties.put("disk",3072);
		cloud_properties.put("disk_format","raw");	
		cloud_properties.put("container_format","bare");
		cloud_properties.put("os_type","linux");		
		cloud_properties.put("os_distro","ubuntu");
		cloud_properties.put("architecture","x86_64");		
		cloud_properties.put("auto_disk_config",true);
		
		//this activates light stemcell support (the original template in cloudstack is copied as a new template)
		cloud_properties.put("light_template","stemcell-3192"); //FIX ME: get from test property

		
		String stemcell_id=cpi.create_stemcell(image_path, cloud_properties);

		
		ResourcePool resource_pool=new ResourcePool();
		resource_pool.compute_offering="m1.small";
		resource_pool.disk=8192;
		resource_pool.ephemeral_disk_offering="Data disk"; 
		
		Networks networks=new Networks();
		Network net=new Network();
		networks.networks.put("default", net);
		//net.type=NetworkType.dynamic;
		net.type=NetworkType.manual;
//		net.ip="10.234.228.155";
//		net.gateway="10.234.228.129";
//		net.netmask="255.255.255.192";
//		net.cloud_properties.put("name","orange-csp10" );//FIX ME: use test props "3112 - preprod - back"
//		net.dns.add("10.234.50.180");
//		net.dns.add("10.234.71.124");
		
		net.ip="10.1.1.5";
		net.gateway="10.1.1.1";
		net.netmask="255.255.255.0";
		net.cloud_properties.put("name","orange-csp10" );//FIX ME: use test props "3112 - preprod - back"
		net.dns.add("8.8.8.8");
		
		
		
		List<String> disk_locality=new ArrayList<String>();
		Map<String, String> env=new HashMap<String, String>();

		String vm_id=cpi.create_vm(agent_id, stemcell_id, resource_pool, networks, disk_locality, env);
		
		boolean hasVM=cpi.has_vm(vm_id);
		
		
		//Integer size=new Integer(10); 
		Integer size=new Integer(100); //ignored if fix disk size offering		
		Map<String, String> diskcloud_properties=new HashMap<String, String>();
		//diskcloud_properties.put("disk_offering","Medium");  //FIXME: use test props
		diskcloud_properties.put("disk_offering","Disk data");
		
		String disk_id=cpi.create_disk(size, diskcloud_properties);
		
		boolean hasDisk=cpi.has_disk(disk_id);
		
		//test set vm metadatas
		Map<String,String> vmsMetadata=new HashMap<String, String>();
		vmsMetadata.put("testVM","true");
		vmsMetadata.put("testOwner","CPIFlowIT test");
		
		cpi.set_vm_metadata(vm_id, vmsMetadata);
 		
		
		//TODO assert disk
		cpi.attach_disk(vm_id, disk_id);


		//reboot the vm
		cpi.reboot_vm(vm_id);
		
		
		//delete flow

		//disk include root disk, so 2 disks + 1 ephemeral disk : 3		
		List<String >disks=cpi.get_disks(vm_id);
		//assertEquals(2, disks.size());

		
		cpi.detach_disk(vm_id, disk_id);

		disks=cpi.get_disks(vm_id);
		//assertEquals(2, disks.size());		
		
		
		//TODO assert hasVM
//		boolean exist=cpi.has_vm(vm_id);
		
		cpi.delete_vm(vm_id);
		cpi.delete_disk(disk_id);
		
		
		cpi.delete_stemcell(stemcell_id);
		
	}

	
	
	private String findZoneId() {
		//TODO: select the exact zone if multiple available
        ListZonesOptions zoneOptions=ListZonesOptions.Builder.available(true);
		Set<Zone> zones = api.getZoneApi().listZones(zoneOptions);
		Assert.notEmpty(zones, "No Zone available");
		Zone zone=zones.iterator().next();
		String zoneId = zone.getId();
		
		Assert.isTrue(zone.getName().equals(this.cloudstackConfig.default_zone));
		
		return zoneId;
	}

}
