package com.orange.oss.cloudfoundry.cscpi;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)

public class UserDataGeneratorTest {

	@Autowired
	UserDataGenerator generator;
	
	@Test
	public void testUserDataGeneration() {
		String userData=this.generator.vmMetaData();
	}

}
