package com.orange.oss.cloudfoundry.cscpi.logic;

import java.util.Map;

import org.jclouds.cloudstack.domain.VirtualMachine;

import com.orange.oss.cloudfoundry.cscpi.domain.Env;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import com.orange.oss.cloudfoundry.cscpi.domain.PersistentDisk;

public interface VmSettingGenerator {

	String createsettingForVM(String agent_id,String vmName, VirtualMachine vm, Networks networks,Env env);
	String updateVmSettingForDisks (String previousSetting, Map<String, PersistentDisk> disks);


}
