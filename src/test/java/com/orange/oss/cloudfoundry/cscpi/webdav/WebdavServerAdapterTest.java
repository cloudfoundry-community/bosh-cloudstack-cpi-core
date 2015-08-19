package com.orange.oss.cloudfoundry.cscpi.webdav;


import static junit.framework.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;





import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.fest.assertions.AssertExtension;
import org.junit.Test;
import org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.orange.oss.cloudfoundry.cscpi.BoshCloudstackCpiCoreApplication;



/**
 * Integration test.
 * Start a webdav server, push file inside, then get it in http
 * @author pierre
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {BoshCloudstackCpiCoreApplication.class})
@WebIntegrationTest({"server.port=8080", "management.port=0"})
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
		
	}

	
}
