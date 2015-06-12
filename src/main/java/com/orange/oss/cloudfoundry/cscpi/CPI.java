package com.orange.oss.cloudfoundry.cscpi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 */
public interface CPI {

    public String create_vm(String agentId, String stemcellId, JsonNode jsonNode);
}
