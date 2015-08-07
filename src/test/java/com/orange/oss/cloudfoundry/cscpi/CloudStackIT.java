package com.orange.oss.cloudfoundry.cscpi;

import java.util.Set;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.Template;
import org.jclouds.cloudstack.features.TemplateApi;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
@ConfigurationProperties
public class CloudStackIT {

	private static Logger logger=LoggerFactory.getLogger(CloudStackIT.class);
	
	@Autowired
	CloudStackApi api;
	
	@Test
	public void testLogin(){

		TemplateApi templateApi = api.getTemplateApi();
		Set<Template> templates = templateApi.listTemplates();
		for (Template t:templates){
			logger.debug("found template {} ",t.getDisplayText());
			logger.debug("template id {}",t.getId());
		}
	}
	
}
