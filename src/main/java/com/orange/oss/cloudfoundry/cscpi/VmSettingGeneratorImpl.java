package com.orange.oss.cloudfoundry.cscpi;

import org.jclouds.cloudstack.domain.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.oss.cloudfoundry.cscpi.domain.Networks;

public class VmSettingGeneratorImpl implements VmSettingGenerator {

	
	private static Logger logger=LoggerFactory.getLogger(VmSettingGeneratorImpl.class.getName());
	
	@Override
	public String settingFor(String vmName, VirtualMachine vm, Networks networks) {
		// TODO Auto-generated method stub
		return "{\"agent_id\":\"xxx\"}";
	}

}
