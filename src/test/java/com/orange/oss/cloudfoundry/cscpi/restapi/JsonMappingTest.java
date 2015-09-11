package com.orange.oss.cloudfoundry.cscpi.restapi;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.BoshCloudstackCpiCoreApplication;
import com.orange.oss.cloudfoundry.cscpi.CPI;
import com.orange.oss.cloudfoundry.cscpi.CPIAdapter;
import com.orange.oss.cloudfoundry.cscpi.domain.CPIResponse;
import com.orange.oss.cloudfoundry.cscpi.exceptions.CpiErrorException;
import com.orange.oss.cloudfoundry.cscpi.exceptions.VMCreationFailedException;
/**
 * json cpi mapping tests. Tests the json => CPI class parameter parsing
 * @author pierre
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {BoshCloudstackCpiCoreApplication.class, MockCPI.class})

public class JsonMappingTest {
	
	
	public class TestData {
		public String request;
		public String response;
	}
	

	@Autowired CPIAdapter adapter;
	@Autowired CPI cpi;
	
	@Test
	public void testLoadJsonFromClasspath() throws IOException{
		String test="create_vm";
		loadData(test);
	}
	
	
	@Test
	public void testCreateStemcell() throws IOException, CpiErrorException{
		TestData data=this.loadData("create_stemcell");
		
		String response = postRequest(data);
		
		Map<String, String> cloud_properties=new HashMap<String, String>();
		String image_path="/home/xx.gz";
		verify(cpi).create_stemcell(image_path, cloud_properties);		
		assertEquals(data.response,response);
		}
	
	@Test
	public void testCreateVM() throws IOException, VMCreationFailedException{
		TestData data=this.loadData("create_vm");
		
		Map<String, String> env=new HashMap<String, String>();
		List<String> disk_locality=new ArrayList<String>();
		String response = postRequest(data);
		
		verify(cpi).create_vm("agent", "stemcell", null, null, disk_locality, env);		
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
		verify(cpi).create_disk(32384, new HashMap<String, String>());
		assertEquals(data.response,response);
		}
	
	
	@Test
	public void testDeleteDisk() throws IOException{
		TestData data=this.loadData("delete_disk");
		
		String response = postRequest(data);
		
		verify(cpi).delete_disk("xxx");
		assertEquals(data.response,response);
		}
	
	

	@Test
	public void testSetVMMetadata() throws IOException{
		TestData data=this.loadData("set_vm_metadata");
		
		Map<String, String> metadata=new HashMap<String, String>();
		metadata.put("director","my-bosh");
		metadata.put("deployment","bosh-cloudstack");
		
		
		String response = postRequest(data);
		
		verify(cpi).set_vm_metadata("urn:vcloud:vm:b068ad18-2903-407d-aeba-c6ff8b721749", metadata);
		assertEquals(data.response,response);
		}
	
	

	/**
	 * util class to load test data + expected data from classpath files
	 * @param test
	 * @throws IOException
	 */
	private TestData loadData(String test) throws IOException {
		TestData data=new TestData();
		data.request=IOUtils.toString(JsonMappingTest.class.getClassLoader().getResourceAsStream("reference/"+test+".json"));
		data.response=IOUtils.toString(JsonMappingTest.class.getClassLoader().getResourceAsStream("reference/"+test+"-response.json"));
		return data;
	}
	
	/**
	 * util class to post REST request
	 * @param data
	 * @return
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 */
	private String postRequest(TestData data) throws JsonProcessingException, IOException {
		 ObjectMapper mapper = new ObjectMapper();
		 JsonNode json = mapper.readTree(data.request);
		CPIResponse resp=this.adapter.execute(json);
		return resp.toString();
}
}
