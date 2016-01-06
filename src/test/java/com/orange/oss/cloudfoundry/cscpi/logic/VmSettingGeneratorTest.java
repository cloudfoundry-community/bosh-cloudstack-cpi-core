package com.orange.oss.cloudfoundry.cscpi.logic;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jclouds.cloudstack.domain.VirtualMachine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.BoshCloudstackCpiCoreApplication;
import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.NetworkType;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import com.orange.oss.cloudfoundry.cscpi.domain.PersistentDisk;
import com.orange.oss.cloudfoundry.cscpi.logic.VmSettingGenerator;


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
		
		Map<String, PersistentDisk> disks=new HashMap<String, PersistentDisk>();
		PersistentDisk d1=new PersistentDisk();
		d1.path="/dev/sdc";
		d1.volumeId="3";
		disks.put("cpidisk-xx", d1); // 1 persistent disk attached
		
		String newSetting=this.generator.updateVmSettingForDisks(setting, disks);
		//Then
	   ObjectMapper mapper = new ObjectMapper();
	   JsonNode updatedSetting = mapper.readTree(newSetting);
	   JsonNode persistentDisk=updatedSetting.get("disks").get("persistent").get("cpidisk-xx");
	   String path=persistentDisk.get("path").toString();
	   assertEquals("\"/dev/sdc\"",path);
	   
	   //Then detach the disk shoud result in intial setting
	   Map<String, PersistentDisk> disksEmpty=new HashMap<String, PersistentDisk>(); // empty, persistent disk detached
	   
	   String settingAfterAtachDetach=this.generator.updateVmSettingForDisks(newSetting, disksEmpty);
	   assertEquals(setting, settingAfterAtachDetach);
	   
	   
	   
	}

	
	
	

}

