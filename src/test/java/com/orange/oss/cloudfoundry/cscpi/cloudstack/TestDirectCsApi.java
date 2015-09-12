package com.orange.oss.cloudfoundry.cscpi.cloudstack;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.orange.oss.cloudfoundry.cscpi.BoshCloudstackCpiCoreApplication;
import com.orange.oss.cloudfoundry.cscpi.config.CloudStackConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)

public class TestDirectCsApi {

	@Autowired
	CloudStackConfiguration config;
	
	@Test
	public void testRawRestApiCall(){
		
		String uri = config.endpoint;
		RestTemplate restTemplate = new RestTemplate();
		String result = restTemplate.getForObject(uri, String.class);
		

		

		
		
	}
}
