package com.orange.oss.cloudfoundry.cscpi.restapi;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
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
import com.orange.oss.cloudfoundry.cscpi.exceptions.CpiErrorException;
/**
 * Rest integration tests.
 * Tests end to end to the running tomcat instance. Checks the rest api, POST verb, correct content-type and accept http headers
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
    int port;
	
	@Autowired
	RestTemplate client;
	

	@Autowired
	CPI cpi;
	
	
	
	@Test
	public void testCreateDisk() throws IOException{
		
		TestData data=this.loadData("create_disk");

		String response = postRequest(data);
		when(cpi.create_disk(anyInt(),any(Map.class))).thenReturn("diskid");
		//verify(cpi).create_disk(32384, new HashMap<String, String>());		
		assertEquals(data.response,response);
		}
	
	@Test
	public void testCreateStemcell() throws IOException, CpiErrorException{
		
		TestData data=this.loadData("create_stemcell");

		HashMap<String, String> cloud_properties=new HashMap<>();
		cloud_properties.put("disk","3072");
		cloud_properties.put("root_device_name","/dev/sda1");
		cloud_properties.put("infrastructure","vcloud");		
		cloud_properties.put("hypervisor","esxi");
		cloud_properties.put("os_type","linux");
		cloud_properties.put("name","bosh-vcloud-esxi-ubuntu-trusty-go_agent");
		cloud_properties.put("disk_format","ovf");				
		cloud_properties.put("os_distro","ubuntu");
		cloud_properties.put("version","3016");
		cloud_properties.put("container_format","bare");				
		cloud_properties.put("architecture","x86_64");
				
		String response = postRequest(data);
		verify(cpi).create_stemcell("/var/vcap/data/tmp/director/stemcell20150731-6669-1s29owb/image", cloud_properties);		
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
