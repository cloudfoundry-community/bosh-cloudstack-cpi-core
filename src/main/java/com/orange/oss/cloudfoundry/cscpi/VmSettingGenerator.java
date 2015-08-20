package com.orange.oss.cloudfoundry.cscpi;

import org.jclouds.cloudstack.domain.VirtualMachine;

import com.orange.oss.cloudfoundry.cscpi.domain.Networks;

public interface VmSettingGenerator {

	String settingFor(String agent_id,String vmName, VirtualMachine vm, Networks networks);

}
