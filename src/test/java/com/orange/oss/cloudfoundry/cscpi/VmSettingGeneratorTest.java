package com.orange.oss.cloudfoundry.cscpi;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jclouds.cloudstack.domain.VirtualMachine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.NetworkType;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)

public class VmSettingGeneratorTest {

	@Autowired
	VmSettingGenerator generator;

	
	
	
	
	
	@Test
	public void test_create_setting() throws JsonProcessingException, IOException {
		
		
		//Given
		Networks networks=new Networks();
		Network net=new Network();
		networks.networks.put("default", net);
		//net.type=NetworkType.dynamic;
		net.type=NetworkType.manual;
		net.ip="10.234.228.158";
		net.gateway="10.234.228.129";
		net.netmask="255.255.255.192";
		net.cloud_properties.put("name", "3112 - preprod - back");
		net.dns.add("10.234.50.180");
		net.dns.add("10.234.71.124");

		
		String agent_id="agent-xxxxxx";
		String vmName="vm-yyyy";
		VirtualMachine vm=null;
		
		
		//When
		String setting=this.generator.createsettingForVM(agent_id, vmName, vm, networks);
		
		//Then
	   ObjectMapper mapper = new ObjectMapper();
	   JsonNode settingObj = mapper.readTree(setting);

	   //network is ok
	   
	   JsonNode network=settingObj.get("networks");
	   
	   //mbus is ok
	   
	   //disk is ok
		
	}
	
	
	@Test
	public void test_update_setting_attach_detach() throws JsonProcessingException, IOException{
		//Given
		Networks networks=new Networks();
		Network net=new Network();
		networks.networks.put("default", net);
		//net.type=NetworkType.dynamic;
		net.type=NetworkType.manual;
		net.ip="10.234.228.158";
		net.gateway="10.234.228.129";
		net.netmask="255.255.255.192";
		net.cloud_properties.put("name", "3112 - preprod - back");
		net.dns.add("10.234.50.180");
		net.dns.add("10.234.71.124");

		
		String agent_id="agent-xxxxxx";
		String vmName="vm-yyyy";
		VirtualMachine vm=null;
		
		
		//When
		String setting=this.generator.createsettingForVM(agent_id, vmName, vm, networks);
		
		String newSetting=this.generator.updateVmSettingForAttachDisk(setting, "new_disk_id");
		//Then
	   ObjectMapper mapper = new ObjectMapper();
	   JsonNode updatedSetting = mapper.readTree(newSetting);
	   JsonNode persistentDisk=updatedSetting.get("disks").get("persistent").get("new_disk_id");
	   String path=persistentDisk.get("path").toString();
	   assertEquals("\"/dev/sdc\"",path);
	   
	   //Then detach the disk shoud result in intial setting
	   
	   String settingAfterAtachDetach=this.generator.updateVmSettingForDetachDisk(newSetting, "new_disk_id");
	   assertEquals(setting, settingAfterAtachDetach);
	   
	   
	   
	}

	
	
	

}

