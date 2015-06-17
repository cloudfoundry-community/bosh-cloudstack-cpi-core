package com.orange.oss.cloudfoundry.cscpi;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 */
public interface CPI {

	String create_vm(String agent_id, String stemcell_id,
			JsonNode resource_pool, JsonNode networks,
			List<String> disk_locality, Map<String, String> env);
}
