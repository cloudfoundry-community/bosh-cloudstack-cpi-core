package com.orange.oss.cloudfoundry.cscpi.cloudstack;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.DiskOffering;
import org.jclouds.cloudstack.options.ListDiskOfferingsOptions;
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
public class TestFindOfferings {

	private static Logger logger=LoggerFactory.getLogger(TestFindOfferings.class.getName());
	
	
	@Autowired
	CloudStackApi api;

	@Test
	public void testListDiskOffering()  {
		
		Set<DiskOffering> listDiskOfferings = api.getOfferingApi().listDiskOfferings(ListDiskOfferingsOptions.NONE);
		for (DiskOffering doffer : listDiskOfferings){
		logger.info("offering {}",doffer.toString());
		}

	}
	
}

