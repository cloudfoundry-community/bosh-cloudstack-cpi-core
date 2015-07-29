package com.orange.oss.cloudfoundry.cscpi.restapi;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.orange.oss.cloudfoundry.cscpi.CPI;
/**
 * Rest integration tests.
 * see  http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html#boot-features-testing-spring-boot-applications
 * @author pierre
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {BoshCloudstackCpiCoreApplication.class, MockCPI.class})
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
	

	@Autowired
	CPI cpi;
	
	@Test
	public void testLoadJsonFromClasspath() throws IOException{
		String test="create_vm";
		loadData(test);
	}
	
	
	@Test
	public void testCreateVM() throws IOException{
		TestData data=this.loadData("create_vm");
		
		
		Map<String, String> env=new HashMap<String, String>();
		List<String> disk_locality=new ArrayList<String>();
		verify(cpi).create_vm("agen", "stemcell", null, null, disk_locality, env);		
		
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

		
		verify(cpi).create_disk(32384, new HashMap<String, String>());

		String response = postRequest(data);		
		assertEquals(data.response,response);
		}
	
	
	@Test
	public void testDeleteDisk() throws IOException{
		TestData data=this.loadData("delete_disk");
		
		verify(cpi).delete_disk("xxx");
		
		String response = postRequest(data);		
		assertEquals(data.response,response);
		}
	
	

	@Test
	public void testSetVMMetadata() throws IOException{
		TestData data=this.loadData("set_vm_metadata");
		
		Map<String, String> metadata=new HashMap<String, String>();
		verify(cpi).set_vm_metadata("vm", metadata);
		
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
