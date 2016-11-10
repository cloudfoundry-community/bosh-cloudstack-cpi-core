package com.orange.oss.cloudfoundry.cscpi.webdav;


import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;



/**
 * Integration test.
 * Start a webdav server, push file inside, then get it in http
 * @author pierre
 *
 */

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=WebEnvironment.DEFINED_PORT)
public class WebdavServerAdapterTest {

	
	@Autowired
	WebdavServerAdapterImpl adapter;
	
	@Test
	public void testPutFileToWebDavServer(){
		
		String content="xxxxxyyyyyyy";
		
		String name="template.vhd";
		String targetUrl=this.adapter.pushFile(new ByteArrayInputStream(content.getBytes()), name);
		
		RestTemplate template=new RestTemplate();
		String retrievedContent=template.getForObject(targetUrl, String.class);
		assertEquals(content,retrievedContent);
		
		this.adapter.delete(name);
	}
	
	@Test
	public void testDeleteShouldNeverFail(){
		this.adapter.delete("toto");
		
	}

	
}
