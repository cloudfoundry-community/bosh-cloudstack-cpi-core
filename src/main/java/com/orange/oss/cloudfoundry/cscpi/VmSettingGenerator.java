package com.orange.oss.cloudfoundry.cscpi;

import org.jclouds.cloudstack.domain.VirtualMachine;

import com.orange.oss.cloudfoundry.cscpi.domain.Networks;

public interface VmSettingGenerator {

	String createsettingForVM(String agent_id,String vmName, VirtualMachine vm, Networks networks);
	String updateVmSettingForAttachDisk (String previousSetting, String disk_id);
	String updateVmSettingForDetachDisk (String previousSetting, String disk_id);	

}
