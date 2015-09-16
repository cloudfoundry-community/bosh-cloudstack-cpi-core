package com.orange.oss.cloudfoundry.cscpi.cloudstack;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

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
	public void testRawRestApiCall() throws NoSuchAlgorithmException, InvalidKeyException {
		Map<String,String> apiParameters=new TreeMap<String, String>();
		apiParameters.put("command","listZones");
		
		String result=this.nativeCsConnector.nativeCall(apiParameters);
		logger.info("response from native call {}",result);

	}
	
	@Test
	public void testRawAsyncApiCall(){
		Map<String,String> apiParameters=new TreeMap<String, String>();
		apiParameters.put("command","listZones");
		
		
		
	}
	
}

