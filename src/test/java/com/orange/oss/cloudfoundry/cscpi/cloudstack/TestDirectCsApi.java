package com.orange.oss.cloudfoundry.cscpi.cloudstack;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.cloudfoundry.cscpi.BoshCloudstackCpiCoreApplication;
import com.orange.oss.cloudfoundry.cspi.cloudstack.NativeCloudstackConnectorImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
public class TestDirectCsApi {

	private static Logger logger=LoggerFactory.getLogger(TestDirectCsApi.class.getName());
	
	
	@Autowired
	NativeCloudstackConnectorImpl nativeCsConnector;

	@Test
	public void testRawRestApiCall()  {
		Map<String,String> apiParameters=new HashMap<String, String>();
		String result=this.nativeCsConnector.nativeCall("listZones",apiParameters);
		logger.info("response from native call {}",result);

	}
	
	@Test
	public void testRawAsyncApiCall(){
		Map<String, String> params=new HashMap<String, String>();
		params.put("name","testdisk");
		params.put("size",Integer.toString(5));
		params.put("diskofferingid", "e921b8e8-f043-491f-9732-9c847fe0fadd"); //"Disk data" offering
		params.put("zoneId","a41b82a0-78d8-4a8f-bb79-303a791bb8a7"); 

		String jobId=this.nativeCsConnector.nativeCall("createVolume",params);
		
		logger.info("disk {} successfully created");
		
	}
	
}

