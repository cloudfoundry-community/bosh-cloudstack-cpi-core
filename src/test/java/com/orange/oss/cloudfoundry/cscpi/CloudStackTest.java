package com.orange.oss.cloudfoundry.cscpi;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.features.SessionApi;
import org.jclouds.cloudstack.features.TemplateApi;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)

public class CloudStackTest {

	@Autowired
	CloudStackApi api;
	
	@Test
	public void testLogin(){

		SessionApi session = api.getSessionApi();
		TemplateApi templateApi = api.getTemplateApi();
		templateApi.listTemplates();
	}
	
}
