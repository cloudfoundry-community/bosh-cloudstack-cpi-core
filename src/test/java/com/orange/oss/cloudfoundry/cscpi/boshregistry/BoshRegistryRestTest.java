package com.orange.oss.cloudfoundry.cscpi.boshregistry;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.orange.oss.cloudfoundry.cscpi.logic.BoshRegistryClient;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=WebEnvironment.DEFINED_PORT)
public class BoshRegistryRestTest {

	@Autowired
	BoshRegistryClient client;
	
	
	
	@Test
	public void testCrud(){
		String  vm_id="xxxx";	
		String settings="zzzz";
		client.put(vm_id,settings);
		String foundSetting=client.getRaw(vm_id);
		assertThat("settings should be equal", settings.equals(foundSetting));
		
		//update
		String updateSetting="wwww";
		client.put(vm_id,updateSetting);
		String updatedFoundSetting=client.getRaw(vm_id);
		assertThat("settings should be equal", updateSetting.equals(updatedFoundSetting));
	}
		
		
		@Test
		public void testSettingSizeLimitation(){
			String  vm_id="xxxx";	
			
			char[] chars = new char[5000];
			Arrays.fill(chars, 'a');
			String settings = new String(chars);
			
			client.put(vm_id,settings);
			String foundSetting=client.getRaw(vm_id);
			assertThat("settings should be equal", settings.equals(foundSetting));
			
			//update
			String updateSetting="wwww";
			client.put(vm_id,updateSetting);
			String updatedFoundSetting=client.getRaw(vm_id);
			assertThat("", updateSetting.equals(updatedFoundSetting));
			
		
		
		client.delete(vm_id);		
	}
	
	@Test(expected=RuntimeException.class)
	public void test_404_if_unknow_vm_id(){
		String  vm_id="xxxx";		
		client.get(vm_id);
	}
	
	@Test
	public void test_delete_is_always_ok(){
		String vm_id="zzzz";
		client.delete(vm_id);
	}
	
	
	
	
	
}
