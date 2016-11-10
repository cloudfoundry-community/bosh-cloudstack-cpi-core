package com.orange.oss.cloudfoundry.cscpi.cloudstack;

import java.util.Set;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.DiskOffering;
import org.jclouds.cloudstack.options.ListDiskOfferingsOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=WebEnvironment.DEFINED_PORT)
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

