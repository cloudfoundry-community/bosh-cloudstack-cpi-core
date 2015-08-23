package com.orange.oss.cloudfoundry.cscpi.boshregistry;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;

import com.orange.oss.cloudfoundry.cscpi.BoshCloudstackCpiCoreApplication;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
@WebIntegrationTest({"server.port=8080"})
public class BoshRegistryRestTest {

	@Autowired
	BoshRegistryClient client;
	
	
	
	@Test
	public void testCrud(){
		String  vm_id="xxxx";	
		String settings="zzzz";
		client.put(vm_id,settings);
		String foundSetting=client.getRaw(vm_id);
		Assert.assertEquals(settings, foundSetting);
		
		//update
		String updateSetting="wwww";
		client.put(vm_id,updateSetting);
		String updatedFoundSetting=client.getRaw(vm_id);
		Assert.assertEquals(updateSetting, updatedFoundSetting);
		
		
		
		
		client.delete(vm_id);		
	}
	
	@Test(expected=HttpClientErrorException.class)
	public void test_404_if_unknow_vm_id(){
		String  vm_id="xxxx";		
		client.get(vm_id);
	}
	
	
	
}
