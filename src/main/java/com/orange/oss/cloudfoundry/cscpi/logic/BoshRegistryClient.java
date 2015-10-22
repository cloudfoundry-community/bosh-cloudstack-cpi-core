package com.orange.oss.cloudfoundry.cscpi.logic;

public interface BoshRegistryClient {

	/**
	 * get settings, i.e. the bosh registry response to bosh agent (maps to /instances/<vm_id>/settings
	 * @param vm_id
	 * @return
	 */
	String get(String vm_id);

	/**
	 * get the raw settings, ie raw setting.json (maps to to /instances/<vm_id>/rawsettings)
	 * @param vm_id
	 * @return
	 */
	String getRaw(String vm_id);

	void put(String vm_id, String settings);

	void delete(String vm_id);
}