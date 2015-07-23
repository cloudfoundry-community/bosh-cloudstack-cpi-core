package com.orange.oss.cloudfoundry.cscpi.restapi;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RestTestContext.class)
@ConfigurationProperties

public class RestApiTest {
	
	
	public class TestData {
		public String request;
		public String response;
	}
	
	
	@Test
	public void testLoadJsonFromClasspath() throws IOException{
		
		String test="createvm";
		
		loadData(test);
		
		
	}

	/**
	 * util class to load test data + expected data from classpath files
	 * @param test
	 * @throws IOException
	 */
	private TestData loadData(String test) throws IOException {
		TestData data=new TestData();
		data.request=IOUtils.toString(RestApiTest.class.getClassLoader().getResourceAsStream(test+".json"));
		data.response=IOUtils.toString(RestApiTest.class.getClassLoader().getResourceAsStream(test+"-response.json"));
		return data;
	}
}
