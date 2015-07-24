package com.orange.oss.cloudfoundry.cscpi.restapi;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    int port=8081;
	
	@Autowired
	RestTemplate client;
	
	
	@Test
	public void testLoadJsonFromClasspath() throws IOException{
		String test="create_vm";
		loadData(test);
	}
	
	
	@Test
	public void testCreateVM() throws IOException{
		TestData data=this.loadData("create_vm");
		String response = postRequest(data);		
		assertEquals(data.response,response);
		}


	
	@Test
	public void testAttachDisk() throws IOException{
		TestData data=this.loadData("attach_disk");
		String response = postRequest(data);		
		assertEquals(data.response,response);
		}

	@Test
	public void testCreateDisk() throws IOException{
		TestData data=this.loadData("create_disk");
		String response = postRequest(data);		
		assertEquals(data.response,response);
		}
	
	
	@Test
	public void testDeleteDisk() throws IOException{
		TestData data=this.loadData("delete_disk");
		String response = postRequest(data);		
		assertEquals(data.response,response);
		}
	
	

	@Test
	public void testSetVMMetadata() throws IOException{
		TestData data=this.loadData("set_vm_metadata");
		String response = postRequest(data);		
		assertEquals(data.response,response);
		}
	
	

	/**
	 * util class to load test data + expected data from classpath files
	 * @param test
	 * @throws IOException
	 */
	private TestData loadData(String test) throws IOException {
		TestData data=new TestData();
		data.request=IOUtils.toString(RestApiTest.class.getClassLoader().getResourceAsStream("reference/"+test+".json"));
		data.response=IOUtils.toString(RestApiTest.class.getClassLoader().getResourceAsStream("reference/"+test+"-response.json"));
		return data;
	}
	
	/**
	 * util class to post REST request
	 * @param data
	 * @return
	 */
	private String postRequest(TestData data) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

		HttpEntity<String> entity = new HttpEntity<String>(data.request,headers);
		String response=this.client.postForEntity("http://localhost:"+port+"/cpi", entity,String.class).getBody();
		return response;
	}
	
}
