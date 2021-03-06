package com.orange.oss.cloudfoundry.cscpi.logic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
public class TestDriveCalculation {

	private static Logger logger=LoggerFactory.getLogger(TestDriveCalculation.class.getName());
	
	@Test
	public void calculateDrivePath(){
		int deviceid=2;
		int driveIndex=deviceid;
		char drive=(char) ('a'+driveIndex);
	    String path="/dev/xvd"+drive;
	    
	    assertEquals("/dev/xvdc", path);

	}
}
