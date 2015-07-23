package com.orange.oss.cloudfoundry.cscpi.restapi;

import java.io.IOException;

import static junit.framework.Assert.*;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.orange.oss.cloudfoundry.cscpi.BoshCloudstackCpiCoreApplication;

/**
 * Rest integration tests.
 * see  http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html#boot-features-testing-spring-boot-applications
 * @author pierre
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
@WebIntegrationTest({"server.port=0", "management.port=0"})

public class RestApiTest {
	
	
	public class TestData {
		public String request;
		public String response;
	}
	
	
	//inject the dynamic http port of the embedded container
	@Value("${local.server.port}")
    int port;
	
	@Autowired
	RestTemplate client;
	
	
	@Test
	public void testLoadJsonFromClasspath() throws IOException{
		String test="createvm";
		loadData(test);
		
	}
	
	
	@Test
	public void testCreateVM() throws IOException{
		TestData data=this.loadData("createvm");

		String response=this.client.postForObject("http://localhost:"+port+"/cpi", data.request, String.class);
		assertEquals(data.response,response);
		
		
		
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
