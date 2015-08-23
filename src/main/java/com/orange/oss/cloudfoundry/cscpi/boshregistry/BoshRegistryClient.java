package com.orange.oss.cloudfoundry.cscpi.boshregistry;

public interface BoshRegistryClient {

	void put(String vm_id, String settings);
	/**
	 * get the bosh registry response to bosh agent (maps to /instances/<vm_id>/settings
	 * @param vm_id
	 * @return
	 */
	String get(String vm_id);
	void delete(String vm_id);
	/**
	 * get the setting.json
	 * @param vm_id
	 * @return
	 */
	String getRaw(String vm_id);
}