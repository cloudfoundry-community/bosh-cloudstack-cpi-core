package com.orange.oss.cloudfoundry.cscpi;

import static org.junit.Assert.*;

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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.NetworkType;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)

public class VmSettingGeneratorTest {

	@Autowired
	VmSettingGenerator generator;
	
	
	@Test
	public void testSettingFor() throws JsonProcessingException, IOException {
		
		
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
		String setting=this.generator.settingFor(agent_id, vmName, vm, networks);
		
		//Then
	   ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
	   JsonNode actualObj = mapper.readTree(setting);

	   //network is ok
	   
	   //mbus is ok
	   
	   //disk is ok
		
	}

}
