package com.orange.oss.cloudfoundry.cscpi.boshregistry;

import junit.framework.Assert;

import org.fest.assertions.AssertExtension;
import org.junit.Test;
import org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.cloudfoundry.cscpi.BoshCloudstackCpiCoreApplication;
import com.orange.oss.cloudfoundry.cscpi.boshregistry.BoshRegistryClient;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
public class BoshRegistryRestTest {

	@Autowired
	BoshRegistryClient client;
	
	
	
	@Test
	public void testCrud(){
		String  vm_id="xxxx";	
		String settings="zzzz";
		client.put(vm_id,settings);
		client.delete(vm_id);
		
		String foundSetting=client.get(vm_id);
		Assert.assertEquals(settings, foundSetting);
	}
	
}
