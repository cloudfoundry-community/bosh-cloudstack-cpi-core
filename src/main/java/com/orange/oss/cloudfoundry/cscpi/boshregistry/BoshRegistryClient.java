package com.orange.oss.cloudfoundry.cscpi.boshregistry;

public interface BoshRegistryClient {

	void put(String vm_id, String settings);
	String get(String vm_id);
}