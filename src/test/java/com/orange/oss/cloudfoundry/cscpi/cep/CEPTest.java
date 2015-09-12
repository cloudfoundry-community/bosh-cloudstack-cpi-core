package com.orange.oss.cloudfoundry.cscpi.cep;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CEPConfig.class)
@ConfigurationProperties

public class CEPTest {

	@Autowired
	CEPInterface cep;
	
	@Test
	public void testCEP() {
		for (int i=0;i<500;i++){
		CPIEvent event=new CPIEvent("create_disk", null, "disk-"+i);
		cep.sendEvent(event);
		}
		
	}

}
